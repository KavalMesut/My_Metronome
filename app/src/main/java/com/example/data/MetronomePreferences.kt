package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight local persistence for Metronome state using SharedPreferences.
 * Zero cold-start overhead, instantaneous reads.
 */
class MetronomePreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "metronome_prefs"
        private const val KEY_BPM = "saved_bpm"
        private const val KEY_BEATS_A = "saved_beats_a"
        private const val KEY_VALUE_B = "saved_value_b"

        const val DEFAULT_BPM = 120
        const val DEFAULT_BEATS_A = 4
        const val DEFAULT_VALUE_B = 4
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBpm(): Int = prefs.getInt(KEY_BPM, DEFAULT_BPM).coerceIn(10, 260)

    fun saveBpm(bpm: Int) {
        prefs.edit().putInt(KEY_BPM, bpm.coerceIn(10, 260)).apply()
    }

    fun getBeatsPerMeasure(): Int =
        prefs.getInt(KEY_BEATS_A, DEFAULT_BEATS_A).coerceIn(1, 20)

    fun saveBeatsPerMeasure(a: Int) {
        prefs.edit().putInt(KEY_BEATS_A, a.coerceIn(1, 20)).apply()
    }

    fun getBeatValue(): Int =
        prefs.getInt(KEY_VALUE_B, DEFAULT_VALUE_B).coerceIn(1, 20)

    fun saveBeatValue(b: Int) {
        prefs.edit().putInt(KEY_VALUE_B, b.coerceIn(1, 20)).apply()
    }
}
