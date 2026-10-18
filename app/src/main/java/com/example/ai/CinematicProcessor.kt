package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import com.example.camera.AspectRatioSetting
import com.example.camera.CinemaFilter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object CinematicProcessor {

    /**
     * Applies full cinematic treatment:
     * 1. 2.39:1 Anamorphic crop & widescreen letterbox bars
     * 2. Depth of Field bokeh blur around focus center
     * 3. Cinematic Color Grading (Teal/Orange, Noir, Golden Hour, etc.)
     * 4. Exposure and Contrast curve
     * 5. Anamorphic lens flare / vignette & film metadata stamp
     */
    fun processCinematic(
        source: Bitmap,
        filter: CinemaFilter,
        fStop: Float = 1.8f, // e.g. 1.4, 1.8, 2.8, 4.0
        exposureEv: Float = 0.0f,
        focusPointNorm: Offset = Offset(0.5f, 0.5f), // Normalized 0..1
        aspectRatioSetting: AspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
        addCinemaStamp: Boolean = true
    ): Bitmap {
        val targetWidth = source.width
        val targetHeight = source.height

        // 1. Create a base bitmap with color grading and exposure
        val graded = applyColorGradeAndExposure(source, filter, exposureEv)

        // 2. Apply simulated Depth of Field (Bokeh) if in shallow aperture (f/1.4 - f/4.0)
        val bokehResult = if (fStop <= 4.5f) {
            applyDepthOfField(graded, focusPointNorm, fStop)
        } else {
            graded
        }

        // 3. Render onto cinema canvas with 2.39:1 letterbox bars
        return applyLetterbox(bokehResult, aspectRatioSetting, fStop, filter, addCinemaStamp)
    }

    private fun applyColorGradeAndExposure(
        src: Bitmap,
        filter: CinemaFilter,
        exposureEv: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Combine Exposure + Contrast + Saturation + Color Tint into ColorMatrix
        val cm = ColorMatrix()

        // Exposure adjustment: EV factor ~ 1.0 + (ev * 0.25)
        val evFactor = 1.0f + (exposureEv * 0.25f).coerceIn(-0.6f, 0.9f)
        val contrast = filter.contrast
        val sat = filter.saturation

        // Set saturation
        cm.setSaturation(sat)

        // Scale contrast & exposure
        val t = (1.0f - contrast) / 2.0f * 255.0f * evFactor
        val scale = contrast * evFactor

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, t,
                0f, scale, 0f, 0f, t,
                0f, 0f, scale, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        // Apply Cinema Color Tint (Teal/Orange shadows & highlights)
        when (filter) {
            CinemaFilter.TEAL_ORANGE -> {
                // Warm highlights (red/yellow boost), cool shadows (cyan/teal boost)
                val tint = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0.00f, 0.00f, 0f, 12f,
                        0.00f, 1.05f, 0.00f, 0f, 4f,
                        0.00f, 0.00f, 1.22f, 0f, 18f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                cm.postConcat(tint)
            }
            CinemaFilter.GOLDEN_HOUR -> {
                val tint = ColorMatrix(
                    floatArrayOf(
                        1.22f, 0.00f, 0.00f, 0f, 22f,
                        0.00f, 1.10f, 0.00f, 0f, 14f,
                        0.00f, 0.00f, 0.88f, 0f, -8f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                cm.postConcat(tint)
            }
            CinemaFilter.FILM_NOIR -> {
                // High contrast dramatic monochrome
                cm.setSaturation(0f)
                val noirBoost = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -15f,
                        0f, 1.35f, 0f, 0f, -15f,
                        0f, 0f, 1.35f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(noirBoost)
            }
            CinemaFilter.CYBERPUNK -> {
                val tint = ColorMatrix(
                    floatArrayOf(
                        1.20f, 0.00f, 0.10f, 0f, 15f,
                        0.00f, 0.95f, 0.00f, 0f, -5f,
                        0.10f, 0.00f, 1.30f, 0f, 24f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                cm.postConcat(tint)
            }
            CinemaFilter.PANAVISION -> {
                val tint = ColorMatrix(
                    floatArrayOf(
                        1.05f, 0.00f, 0.00f, 0f, 8f,
                        0.00f, 1.02f, 0.00f, 0f, 4f,
                        0.00f, 0.00f, 0.98f, 0f, 2f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                cm.postConcat(tint)
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)

        // Subtle cinema vignette
        applyCinemaVignette(canvas, src.width, src.height)

        return output
    }

    private fun applyCinemaVignette(canvas: Canvas, width: Int, height: Int) {
        val radius = max(width, height) * 0.75f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = RadialGradient(
            width / 2f,
            height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.argb(40, 0, 0, 0), Color.argb(160, 0, 0, 0)),
            floatArrayOf(0.4f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * Creates a soft Bokeh depth-of-field effect around focus point.
     * Uses a fast stack blur approximation scaled down for efficiency and smoothness.
     */
    private fun applyDepthOfField(
        sharpBitmap: Bitmap,
        focusPointNorm: Offset,
        fStop: Float
    ): Bitmap {
        val width = sharpBitmap.width
        val height = sharpBitmap.height

        // Bokeh intensity scales inversely with fStop (e.g. f/1.4 has strongest blur)
        val blurRadius = ((4.5f - fStop) / 3.0f * 20f).coerceIn(4f, 24f).toInt()
        if (blurRadius <= 0) return sharpBitmap

        // Downscale for smooth fast blur
        val downscale = 4
        val smallW = (width / downscale).coerceAtLeast(10)
        val smallH = (height / downscale).coerceAtLeast(10)
        val smallBitmap = Bitmap.createScaledBitmap(sharpBitmap, smallW, smallH, true)

        // Apply fast blur to small bitmap
        val blurredSmall = fastBlur(smallBitmap, blurRadius)
        val blurredFull = Bitmap.createScaledBitmap(blurredSmall, width, height, true)

        // Composite sharp subject over blurred background with radial alpha mask
        val composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)

        // 1. Draw blurred layer as background
        canvas.drawBitmap(blurredFull, 0f, 0f, null)

        // 2. Draw sharp bitmap with radial alpha gradient mask centered at focus point
        val focusX = (focusPointNorm.x * width).coerceIn(0f, width.toFloat())
        val focusY = (focusPointNorm.y * height).coerceIn(0f, height.toFloat())
        val clearRadius = min(width, height) * 0.35f

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)

        // Sharp layer canvas
        val sharpWithMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val sharpCanvas = Canvas(sharpWithMask)
        sharpCanvas.drawBitmap(sharpBitmap, 0f, 0f, null)

        val alphaMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                focusX,
                focusY,
                clearRadius * 1.5f,
                intArrayOf(Color.BLACK, Color.argb(180, 0, 0, 0), Color.TRANSPARENT),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        sharpCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), alphaMaskPaint)

        // Draw sharp masked subject over the blur
        canvas.drawBitmap(sharpWithMask, 0f, 0f, null)

        return composite
    }

    /**
     * Widescreen 2.39:1 Letterbox Bars (Cinemascope / Anamorphic format)
     */
    private fun applyLetterbox(
        image: Bitmap,
        ratioSetting: AspectRatioSetting,
        fStop: Float,
        filter: CinemaFilter,
        addStamp: Boolean
    ): Bitmap {
        val width = image.width
        val height = image.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Black canvas
        canvas.drawColor(Color.BLACK)

        // Calculate active cinema picture rect
        val targetRatio = when (ratioSetting) {
            AspectRatioSetting.RATIO_CINEMA -> 2.39f
            AspectRatioSetting.RATIO_16_9 -> 16f / 9f
            AspectRatioSetting.RATIO_4_3 -> 4f / 3f
        }

        val currentRatio = width.toFloat() / height.toFloat()

        val activeRect = if (currentRatio > targetRatio) {
            // Picture is wider than target ratio: pillarbox
            val activeWidth = (height * targetRatio).roundToInt()
            val left = (width - activeWidth) / 2
            Rect(left, 0, left + activeWidth, height)
        } else {
            // Picture is taller than target ratio: letterbox (bars on top and bottom)
            val activeHeight = (width / targetRatio).roundToInt()
            val top = (height - activeHeight) / 2
            Rect(0, top, width, top + activeHeight)
        }

        // Draw image scaled / cropped to active rect
        canvas.drawBitmap(image, null, activeRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Draw cinema letterbox top & bottom bars
        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        if (activeRect.top > 0) {
            // Top black bar
            canvas.drawRect(0f, 0f, width.toFloat(), activeRect.top.toFloat(), barPaint)
            // Bottom black bar
            canvas.drawRect(0f, activeRect.bottom.toFloat(), width.toFloat(), height.toFloat(), barPaint)
        }

        if (activeRect.left > 0) {
            canvas.drawRect(0f, 0f, activeRect.left.toFloat(), height.toFloat(), barPaint)
            canvas.drawRect(activeRect.right.toFloat(), 0f, width.toFloat(), height.toFloat(), barPaint)
        }

        // 4. Subtle cinema stamp metadata in the letterbox bar or bottom corner
        if (addStamp && ratioSetting == AspectRatioSetting.RATIO_CINEMA) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 255, 255, 255)
                textSize = (height * 0.022f).coerceIn(24f, 44f)
                letterSpacing = 0.15f
                isFakeBoldText = true
            }

            val yellowAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 204, 0) // Apple yellow
                textSize = (height * 0.022f).coerceIn(24f, 44f)
                letterSpacing = 0.15f
                isFakeBoldText = true
            }

            val stampY = if (activeRect.bottom < height - 20) {
                activeRect.bottom + (height - activeRect.bottom) / 1.7f
            } else {
                height - 30f
            }

            val leftText = "CINEMATIC 2.39:1  •  ${filter.displayName.uppercase()}  •  f/${fStop}"
            canvas.drawText(leftText, 48f, stampY, textPaint)

            val rightText = "35MM ANAMORPHIC"
            val rightWidth = yellowAccentPaint.measureText(rightText)
            canvas.drawText(rightText, width - rightWidth - 48f, stampY, yellowAccentPaint)
        }

        return result
    }

    /**
     * Fast box blur algorithm for Android bitmap.
     */
    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) {
            dv[i] = i / divsum
        }

        var yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (y in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (i in -radius..radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (i in -radius..radius) {
                yi = max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
            }
            yi = x
            stackpointer = radius
            for (y in 0 until h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
