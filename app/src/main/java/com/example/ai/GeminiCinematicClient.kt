package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.camera.AspectRatioSetting
import com.example.camera.CinemaFilter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseModalities: List<String>? = null,
    val imageConfig: GeminiImageConfig? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    val aspectRatio: String = "16:9",
    val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateCinematicImage(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeCinematicScene(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiProcessResult(
    val resultBitmap: Bitmap,
    val isFromAi: Boolean,
    val aiDescription: String
)

object GeminiCinematicClient {
    private const val TAG = "GeminiCinematicClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    /**
     * Converts Bitmap to JPEG base64 string
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val stream = ByteArrayOutputStream()
        // Resize if too huge to keep latency low
        val scaled = if (bitmap.width > 1280 || bitmap.height > 1280) {
            val scale = 1280f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Enhances a photo with cinematic style.
     * Tries Gemini API if key is present; applies high-end local 2.39:1 widescreen letterbox,
     * bokeh and color grade seamlessly.
     */
    suspend fun enhancePhotoCinematic(
        source: Bitmap,
        filter: CinemaFilter,
        fStop: Float = 1.8f,
        exposureEv: Float = 0.0f
    ): GeminiProcessResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val prompt = "Transform this photo into an iconic cinematic still: apply ${filter.displayName} film color grading (${filter.description}), dramatic movie lighting, deep shadows, cinematic bokeh depth-of-field, and Hollywood widescreen composition."

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No custom Gemini API key configured in Secrets. Running local high-fidelity cinematic pipeline.")
            val processed = CinematicProcessor.processCinematic(
                source = source,
                filter = filter,
                fStop = fStop,
                exposureEv = exposureEv,
                aspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
                addCinemaStamp = true
            )
            return@withContext GeminiProcessResult(
                resultBitmap = processed,
                isFromAi = false,
                aiDescription = "Modo Cinemático 2.39:1 com ${filter.displayName} e abertura f/$fStop"
            )
        }

        try {
            val base64Img = bitmapToBase64(source)
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Img))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = GeminiImageConfig(aspectRatio = "16:9", imageSize = "1K")
                )
            )

            val response = api.generateCinematicImage(apiKey, request)
            var generatedBitmap: Bitmap? = null
            var textDescription = ""

            val parts = response.candidates?.firstOrNull()?.content?.parts
            if (parts != null) {
                for (part in parts) {
                    if (part.inlineData != null && part.inlineData.data.isNotBlank()) {
                        val decodedBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                        generatedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }
                    if (!part.text.isNullOrBlank()) {
                        textDescription = part.text
                    }
                }
            }

            if (generatedBitmap != null) {
                // Apply 2.39:1 letterbox onto the AI generated result
                val finalLetterbox = CinematicProcessor.processCinematic(
                    source = generatedBitmap,
                    filter = filter,
                    fStop = fStop,
                    exposureEv = exposureEv,
                    aspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
                    addCinemaStamp = true
                )
                GeminiProcessResult(
                    resultBitmap = finalLetterbox,
                    isFromAi = true,
                    aiDescription = "Aprimorado com Gemini IA • Iluminação e tom cinematográfico 2.39:1"
                )
            } else {
                // If model returned text commentary instead of raw image, apply local processor
                val processed = CinematicProcessor.processCinematic(
                    source = source,
                    filter = filter,
                    fStop = fStop,
                    exposureEv = exposureEv,
                    aspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
                    addCinemaStamp = true
                )
                GeminiProcessResult(
                    resultBitmap = processed,
                    isFromAi = true,
                    aiDescription = if (textDescription.isNotBlank()) textDescription.take(120) else "Modo Cinemático 2.39:1 • IA ativada"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error, falling back to local cinematic engine: ${e.message}")
            val processed = CinematicProcessor.processCinematic(
                source = source,
                filter = filter,
                fStop = fStop,
                exposureEv = exposureEv,
                aspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
                addCinemaStamp = true
            )
            GeminiProcessResult(
                resultBitmap = processed,
                isFromAi = false,
                aiDescription = "Processamento cinematográfico local ativo (2.39:1 Anamórfico)"
            )
        }
    }
}
