package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import com.example.audio.WoodblockGenerator
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

/**
 * High-precision, ultra-low latency, sample-accurate Metronome Audio Engine.
 *
 * Utilizes streaming 16-bit PCM AudioTrack with monotonic sample tracking
 * to ensure 0.00ms timing drift over indefinite playback duration.
 */
class MetronomeEngine(
    private val context: Context? = null,
    private val onBeat: ((beatIndex: Int, isAccent: Boolean) -> Unit)? = null
) {
    companion object {
        const val SAMPLE_RATE = WoodblockGenerator.SAMPLE_RATE
        const val MIN_BPM = 10
        const val MAX_BPM = 260
        const val MIN_BEATS = 1
        const val MAX_BEATS = 20
        const val CHUNK_SIZE = 512 // ~11.6ms buffer chunks for minimal latency
    }

    // Pre-computed dry acoustic woodblock samples in memory
    private val accentBuffer: ShortArray = WoodblockGenerator.generateAccentClick()
    private val normalBuffer: ShortArray = WoodblockGenerator.generateNormalClick()

    // Thread-safe parameters
    private val bpm = AtomicInteger(120)
    private val beatsPerMeasure = AtomicInteger(4)
    private val isRunning = AtomicBoolean(false)

    // Audio thread reference
    @Volatile
    private var audioThread: Thread? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    // Audio focus listener
    private var audioManager: AudioManager? = null
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    init {
        context?.let { ctx ->
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        stop()
                    }
                }
            }
        }
    }

    fun isPlaying(): Boolean = isRunning.get()

    fun getBpm(): Int = bpm.get()

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(MIN_BPM, MAX_BPM)
        bpm.set(clamped)
    }

    fun getBeatsPerMeasure(): Int = beatsPerMeasure.get()

    fun setBeatsPerMeasure(newBeats: Int) {
        val clamped = newBeats.coerceIn(MIN_BEATS, MAX_BEATS)
        beatsPerMeasure.set(clamped)
    }

    /**
     * Starts the metronome immediately on beat 1 (Accent).
     */
    @Synchronized
    fun start() {
        if (isRunning.get()) return

        // Request audio focus
        requestAudioFocus()

        isRunning.set(true)

        val thread = Thread({
            runAudioLoop()
        }, "MetronomeAudioThread")

        audioThread = thread
        thread.start()
    }

    /**
     * Stops the metronome immediately and cleans up audio buffers.
     */
    @Synchronized
    fun stop() {
        if (!isRunning.getAndSet(false)) return

        abandonAudioFocus()

        val track = audioTrack
        audioTrack = null
        try {
            track?.pause()
            track?.flush()
            track?.stop()
            track?.release()
        } catch (_: Exception) {}

        audioThread?.interrupt()
        audioThread = null
    }

    /**
     * Main sample-accurate streaming audio loop.
     */
    private fun runAudioLoop() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (_: Exception) {}

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufSize, CHUNK_SIZE * 4)

        val track = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }
        } catch (e: Exception) {
            isRunning.set(false)
            return
        }

        audioTrack = track
        try {
            track.play()
        } catch (e: Exception) {
            isRunning.set(false)
            return
        }

        val chunk = ShortArray(CHUNK_SIZE)

        // Monotonic reference tracking for zero-drift calculation
        var totalSampleCount: Long = 0L
        var baseSampleRef: Long = 0L
        var beatsSinceBase: Long = 0L
        var lastUsedBpm = bpm.get()
        var currentBeatInMeasure = 0

        // Active sound rendering pointer
        var activeSample: ShortArray? = null
        var activeSampleOffset = 0

        // Calculate next beat sample target
        var nextBeatSampleTarget: Long = 0L

        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            val currentBpm = bpm.get()
            val currentBeats = beatsPerMeasure.get()

            // If BPM changed, re-anchor base reference to avoid phase jump or drift
            if (currentBpm != lastUsedBpm) {
                baseSampleRef = totalSampleCount
                beatsSinceBase = 0L
                lastUsedBpm = currentBpm
                val samplesPerBeat = (SAMPLE_RATE.toDouble() * 60.0) / currentBpm
                nextBeatSampleTarget = baseSampleRef + samplesPerBeat.roundToLong()
            }

            // Fill chunk with sample-accurate mixing
            for (i in 0 until CHUNK_SIZE) {
                if (totalSampleCount >= nextBeatSampleTarget) {
                    val beatIndex = currentBeatInMeasure % currentBeats
                    val isAccent = (beatIndex == 0)

                    activeSample = if (isAccent) accentBuffer else normalBuffer
                    activeSampleOffset = 0

                    // Fast UI callback
                    onBeat?.invoke(beatIndex, isAccent)

                    currentBeatInMeasure = (beatIndex + 1) % currentBeats
                    beatsSinceBase++

                    // Absolute sample position calculation: base + (k * 60 * Fs / BPM)
                    val samplesPerBeat = (SAMPLE_RATE.toDouble() * 60.0) / currentBpm
                    nextBeatSampleTarget = baseSampleRef + (beatsSinceBase.toDouble() * samplesPerBeat).roundToLong()
                }

                // Mix active woodblock sample into output
                val sampleValue: Short = if (activeSample != null && activeSampleOffset < activeSample!!.size) {
                    val s = activeSample!![activeSampleOffset++]
                    if (activeSampleOffset >= activeSample!!.size) {
                        activeSample = null
                    }
                    s
                } else {
                    0
                }

                chunk[i] = sampleValue
                totalSampleCount++
            }

            val written = track.write(chunk, 0, CHUNK_SIZE)
            if (written < 0) {
                break
            }
        }

        try {
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener ?: return)
                    .build()
                am.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        audioManager?.let { am ->
            audioFocusChangeListener?.let { listener ->
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(listener)
            }
        }
    }

    fun release() {
        stop()
    }
}
