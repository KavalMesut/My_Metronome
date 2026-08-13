package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.MetronomePreferences
import com.example.engine.MetronomeEngine
import com.example.ui.MetronomeScreen
import com.example.ui.theme.MetronomeTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferences: MetronomePreferences
    private lateinit var engine: MetronomeEngine

    // State holders
    private var isPlayingState by mutableStateOf(false)
    private var currentBeatIndexState by mutableIntStateOf(0)
    private var isCurrentAccentState by mutableStateOf(false)
    private var lastBeatTriggerTimeState by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = MetronomePreferences(applicationContext)

        // Initialize metronome engine with beat callback
        engine = MetronomeEngine(
            context = applicationContext,
            onBeat = { beatIndex, isAccent ->
                runOnUiThread {
                    currentBeatIndexState = beatIndex
                    isCurrentAccentState = isAccent
                    lastBeatTriggerTimeState = System.currentTimeMillis()
                }
            }
        )

        // Load saved state
        val initialBpm = preferences.getBpm()
        val initialA = preferences.getBeatsPerMeasure()
        val initialB = preferences.getBeatValue()

        engine.setBpm(initialBpm)
        engine.setBeatsPerMeasure(initialA)

        setContent {
            MetronomeTheme {
                var bpm by remember { mutableIntStateOf(initialBpm) }
                var beatsPerMeasure by remember { mutableIntStateOf(initialA) }
                var beatValue by remember { mutableIntStateOf(initialB) }

                MetronomeScreen(
                    bpm = bpm,
                    beatsPerMeasure = beatsPerMeasure,
                    beatValue = beatValue,
                    isPlaying = isPlayingState,
                    currentBeatIndex = currentBeatIndexState,
                    isCurrentAccent = isCurrentAccentState,
                    lastBeatTriggerTime = lastBeatTriggerTimeState,
                    onBpmChange = { newBpm ->
                        val clamped = newBpm.coerceIn(10, 260)
                        bpm = clamped
                        engine.setBpm(clamped)
                        preferences.saveBpm(clamped)
                    },
                    onBeatsPerMeasureChange = { newA ->
                        val clamped = newA.coerceIn(1, 20)
                        beatsPerMeasure = clamped
                        engine.setBeatsPerMeasure(clamped)
                        preferences.saveBeatsPerMeasure(clamped)
                    },
                    onBeatValueChange = { newB ->
                        val clamped = newB.coerceIn(1, 20)
                        beatValue = clamped
                        preferences.saveBeatValue(clamped)
                    },
                    onTogglePlay = {
                        togglePlayback()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        }
    }

    private fun togglePlayback() {
        if (isPlayingState) {
            stopMetronome()
        } else {
            startMetronome()
        }
    }

    private fun startMetronome() {
        isPlayingState = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        engine.start()
    }

    private fun stopMetronome() {
        isPlayingState = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        engine.stop()
        currentBeatIndexState = 0
        isCurrentAccentState = false
        lastBeatTriggerTimeState = 0L
    }

    override fun onPause() {
        super.onPause()
        if (isPlayingState) {
            stopMetronome()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
