package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.WoodblockGenerator
import com.example.data.MetronomePreferences
import com.example.engine.MetronomeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Metronome", appName)
    }

    @Test
    fun `preferences default and save values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = MetronomePreferences(context)

        assertEquals(120, prefs.getBpm())
        assertEquals(4, prefs.getBeatsPerMeasure())
        assertEquals(4, prefs.getBeatValue())

        prefs.saveBpm(160)
        assertEquals(160, prefs.getBpm())

        prefs.saveBeatsPerMeasure(7)
        assertEquals(7, prefs.getBeatsPerMeasure())

        prefs.saveBeatValue(8)
        assertEquals(8, prefs.getBeatValue())
    }

    @Test
    fun `woodblock generator generates valid non-empty audio samples`() {
        val accent = WoodblockGenerator.generateAccentClick()
        val normal = WoodblockGenerator.generateNormalClick()

        assertTrue(accent.isNotEmpty())
        assertTrue(normal.isNotEmpty())

        // Ensure non-zero energy
        assertTrue(accent.any { it != 0.toShort() })
        assertTrue(normal.any { it != 0.toShort() })
    }

    @Test
    fun `metronome engine bpm limits and measure clamping`() {
        val engine = MetronomeEngine()

        engine.setBpm(5) // Below MIN_BPM (10)
        assertEquals(10, engine.getBpm())

        engine.setBpm(300) // Above MAX_BPM (260)
        assertEquals(260, engine.getBpm())

        engine.setBpm(144)
        assertEquals(144, engine.getBpm())

        engine.setBeatsPerMeasure(0) // Below MIN_BEATS (1)
        assertEquals(1, engine.getBeatsPerMeasure())

        engine.setBeatsPerMeasure(25) // Above MAX_BEATS (20)
        assertEquals(20, engine.getBeatsPerMeasure())

        assertFalse(engine.isPlaying())
    }
}
