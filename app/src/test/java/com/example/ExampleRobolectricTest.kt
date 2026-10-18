package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ai.CinematicProcessor
import com.example.camera.AspectRatioSetting
import com.example.camera.CinemaFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("iCamera", appName)
    }

    @Test
    fun testCinematicProcessingPipeline() {
        val testBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val result = CinematicProcessor.processCinematic(
            source = testBitmap,
            filter = CinemaFilter.TEAL_ORANGE,
            fStop = 1.8f,
            exposureEv = 0.5f,
            aspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
            addCinemaStamp = true
        )

        assertNotNull(result)
        assertEquals(400, result.width)
        assertEquals(300, result.height)
    }
}
