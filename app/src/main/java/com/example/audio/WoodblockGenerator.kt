package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates natural, dry acoustic woodblock / wood click PCM audio samples.
 * No external audio files or disk I/O required; generates in < 2ms during startup.
 */
object WoodblockGenerator {
    const val SAMPLE_RATE = 44100

    /**
     * Generates a high-pitch, crisp accent woodblock click (~22ms).
     */
    fun generateAccentClick(): ShortArray {
        val durationMs = 22.0
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 1450.0 // Fundamental resonant mode
        val f2 = 2320.0 // Secondary wood mode (1.6x)
        val f3 = 3480.0 // Tertiary mode (2.4x)
        val decayTau = 0.0050 // Decay constant (seconds)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t / decayTau)

            // Sharp transient strike on the first 1.5ms
            val transient = if (t < 0.0015) {
                sin(2.0 * PI * 4200.0 * t) * (1.0 - t / 0.0015) * 0.4
            } else {
                0.0
            }

            // Resonant modal frequencies of dry hardwood
            val mode1 = sin(2.0 * PI * f1 * t) * 0.55
            val mode2 = sin(2.0 * PI * f2 * t) * 0.25
            val mode3 = sin(2.0 * PI * f3 * t) * 0.12

            val sampleValue = (mode1 + mode2 + mode3 + transient) * envelope
            val clamped = (sampleValue * 30000.0).coerceIn(-32767.0, 32767.0)
            buffer[i] = clamped.toInt().toShort()
        }
        return buffer
    }

    /**
     * Generates a warm, natural regular wood click (~18ms).
     */
    fun generateNormalClick(): ShortArray {
        val durationMs = 18.0
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 920.0  // Fundamental resonant mode
        val f2 = 1480.0 // Secondary wood mode (1.6x)
        val f3 = 2200.0 // Tertiary mode (2.4x)
        val decayTau = 0.0042 // Decay constant (seconds)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t / decayTau)

            // Sharp transient strike
            val transient = if (t < 0.0012) {
                sin(2.0 * PI * 3100.0 * t) * (1.0 - t / 0.0012) * 0.35
            } else {
                0.0
            }

            // Resonant modal frequencies of dry wood
            val mode1 = sin(2.0 * PI * f1 * t) * 0.60
            val mode2 = sin(2.0 * PI * f2 * t) * 0.22
            val mode3 = sin(2.0 * PI * f3 * t) * 0.10

            val sampleValue = (mode1 + mode2 + mode3 + transient) * envelope
            val clamped = (sampleValue * 26000.0).coerceIn(-32767.0, 32767.0)
            buffer[i] = clamped.toInt().toShort()
        }
        return buffer
    }
}
