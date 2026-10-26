package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

enum class SoundscapeType(val displayName: String, val subtitle: String) {
    NONE("Mute", "Silent focus"),
    BROWN_NOISE("Brown Noise", "Deep soothing rumble for deep work"),
    BINAURAL_ALPHA("Binaural Alpha", "10Hz cognitive flow frequency"),
    WHITE_NOISE("White Noise", "Even frequency distraction blocker")
}

class AmbientSoundGenerator {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentType = SoundscapeType.NONE

    fun setSoundscape(type: SoundscapeType) {
        if (currentType == type && isPlaying) return
        currentType = type
        stop()

        if (type != SoundscapeType.NONE) {
            start(type)
        }
    }

    private fun start(type: SoundscapeType) {
        isPlaying = true
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playbackJob = scope.launch {
            val random = Random()
            val chunkSamples = 2048
            val buffer = ShortArray(chunkSamples * 2) // Stereo (left, right)
            var lastBrownL = 0.0
            var lastBrownR = 0.0
            var phaseL = 0.0
            var phaseR = 0.0

            val baseFreq = 210.0 // Hz
            val beatFreq = 10.0 // 10Hz Alpha difference
            val freqL = baseFreq
            val freqR = baseFreq + beatFreq

            val volume = 0.35f

            while (isActive && isPlaying) {
                when (type) {
                    SoundscapeType.BROWN_NOISE -> {
                        for (i in 0 until chunkSamples) {
                            val whiteL = (random.nextDouble() * 2.0 - 1.0)
                            val whiteR = (random.nextDouble() * 2.0 - 1.0)
                            // Leaky integrator for true brownian 1/f^2 curve
                            lastBrownL = (lastBrownL + (0.02 * whiteL)) / 1.02
                            lastBrownR = (lastBrownR + (0.02 * whiteR)) / 1.02
                            val sampleL = (lastBrownL * 3.5 * volume * Short.MAX_VALUE)
                                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                                .toInt().toShort()
                            val sampleR = (lastBrownR * 3.5 * volume * Short.MAX_VALUE)
                                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                                .toInt().toShort()
                            buffer[i * 2] = sampleL
                            buffer[i * 2 + 1] = sampleR
                        }
                    }
                    SoundscapeType.BINAURAL_ALPHA -> {
                        for (i in 0 until chunkSamples) {
                            val sampleL = (sin(phaseL) * volume * 0.7 * Short.MAX_VALUE).toInt().toShort()
                            val sampleR = (sin(phaseR) * volume * 0.7 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i * 2] = sampleL
                            buffer[i * 2 + 1] = sampleR

                            phaseL += 2.0 * PI * freqL / sampleRate
                            if (phaseL >= 2.0 * PI) phaseL -= 2.0 * PI

                            phaseR += 2.0 * PI * freqR / sampleRate
                            if (phaseR >= 2.0 * PI) phaseR -= 2.0 * PI
                        }
                    }
                    SoundscapeType.WHITE_NOISE -> {
                        for (i in 0 until chunkSamples) {
                            val sampleL = ((random.nextDouble() * 2.0 - 1.0) * volume * 0.3 * Short.MAX_VALUE).toInt().toShort()
                            val sampleR = ((random.nextDouble() * 2.0 - 1.0) * volume * 0.3 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i * 2] = sampleL
                            buffer[i * 2 + 1] = sampleR
                        }
                    }
                    SoundscapeType.NONE -> {
                        break
                    }
                }

                val written = audioTrack?.write(buffer, 0, buffer.size) ?: 0
                if (written < 0) {
                    break
                }
            }
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
