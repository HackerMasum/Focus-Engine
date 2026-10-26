package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class SoundscapeType(
    val displayName: String,
    val subtitle: String,
    val iconEmoji: String
) {
    NONE("Mute", "Silent focus session", "🔇"),
    BROWN_NOISE("Deep Brown", "Warm sub-frequency rumble for intense concentration", "🤎"),
    PINK_NOISE("Pink Noise", "Balanced 1/f natural frequency, improves memory retention", "🌸"),
    WHITE_NOISE("White Noise", "Even spectrum noise to mask sudden distractions", "⚪"),
    BINAURAL_BETA("Beta Flow (16Hz)", "High alertness, ideal for coding, writing & math", "⚡"),
    BINAURAL_ALPHA("Alpha Waves (10Hz)", "Calm alertness, cognitive flow state & retention", "🧠"),
    BINAURAL_THETA("Theta Waves (6Hz)", "Deep creative insight, design & meditation", "🌌"),
    RAIN_SHOWER("Forest Rain", "Procedural soothing rainfall & gentle drops", "🌧️"),
    ZEN_BOWL("Zen 432Hz Drone", "Harmonic golden-ratio resonance for inner peace", "🧘"),
    FIRE_HEARTH("Warm Hearth", "Cosy fireside crackling embers", "🔥"),
    CUSTOM("Custom Audio", "User-imported audio track", "🎵")
}

class AmbientSoundGenerator {
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentType = SoundscapeType.NONE

    @Volatile
    private var currentCustomPath: String? = null

    @Volatile
    var volumeLevel: Float = 0.5f
        set(value) {
            field = value.coerceIn(0.05f, 1.0f)
            mediaPlayer?.setVolume(field, field)
        }

    fun setSoundscape(
        type: SoundscapeType,
        context: Context? = null,
        customFilePath: String? = null
    ) {
        if (currentType == type && isPlaying && (type != SoundscapeType.CUSTOM || currentCustomPath == customFilePath)) {
            return
        }

        currentType = type
        currentCustomPath = customFilePath
        stop()

        if (type == SoundscapeType.NONE) return

        if (type == SoundscapeType.CUSTOM && !customFilePath.isNullOrBlank() && context != null) {
            startCustomAudio(context, customFilePath)
        } else if (type != SoundscapeType.CUSTOM) {
            startProcedural(type)
        }
    }

    private fun startCustomAudio(context: Context, path: String) {
        try {
            val file = File(path)
            mediaPlayer = MediaPlayer().apply {
                if (file.exists()) {
                    setDataSource(path)
                } else {
                    setDataSource(context, Uri.parse(path))
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(volumeLevel, volumeLevel)
                prepare()
                start()
            }
            isPlaying = true
        } catch (_: Exception) {
            isPlaying = false
        }
    }

    private fun startProcedural(type: SoundscapeType) {
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
            val buffer = ShortArray(chunkSamples * 2)

            var lastBrownL = 0.0
            var lastBrownR = 0.0

            // Pink noise Paul Kellet filters
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0

            var phaseL = 0.0
            var phaseR = 0.0

            // Rain & fire state
            var rainDropCounter = 0
            var crackleCounter = 0

            val baseCarrier = 216.0 // Hz
            val beatDiff = when (type) {
                SoundscapeType.BINAURAL_BETA -> 16.0 // Beta (16Hz)
                SoundscapeType.BINAURAL_ALPHA -> 10.0 // Alpha (10Hz)
                SoundscapeType.BINAURAL_THETA -> 6.0 // Theta (6Hz)
                else -> 10.0
            }

            val freqL = baseCarrier
            val freqR = baseCarrier + beatDiff

            while (isActive && isPlaying) {
                val vol = volumeLevel

                when (type) {
                    SoundscapeType.BROWN_NOISE -> {
                        for (i in 0 until chunkSamples) {
                            val whiteL = random.nextDouble() * 2.0 - 1.0
                            val whiteR = random.nextDouble() * 2.0 - 1.0
                            lastBrownL = (lastBrownL + (0.02 * whiteL)) / 1.02
                            lastBrownR = (lastBrownR + (0.02 * whiteR)) / 1.02

                            val sL = (lastBrownL * 3.5 * vol * Short.MAX_VALUE).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                            val sR = (lastBrownR * 3.5 * vol * Short.MAX_VALUE).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                            buffer[i * 2] = sL
                            buffer[i * 2 + 1] = sR
                        }
                    }

                    SoundscapeType.PINK_NOISE -> {
                        for (i in 0 until chunkSamples) {
                            val white = (random.nextDouble() * 2.0 - 1.0)
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
                            b6 = white * 0.115926

                            val s = (pink * vol * 0.6 * Short.MAX_VALUE).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                            buffer[i * 2] = s
                            buffer[i * 2 + 1] = s
                        }
                    }

                    SoundscapeType.WHITE_NOISE -> {
                        for (i in 0 until chunkSamples) {
                            val sL = ((random.nextDouble() * 2.0 - 1.0) * vol * 0.28 * Short.MAX_VALUE).toInt().toShort()
                            val sR = ((random.nextDouble() * 2.0 - 1.0) * vol * 0.28 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i * 2] = sL
                            buffer[i * 2 + 1] = sR
                        }
                    }

                    SoundscapeType.BINAURAL_ALPHA,
                    SoundscapeType.BINAURAL_BETA,
                    SoundscapeType.BINAURAL_THETA -> {
                        for (i in 0 until chunkSamples) {
                            // Warm carrier + binaural stereo difference
                            val sL = (sin(phaseL) * vol * 0.6 * Short.MAX_VALUE).toInt().toShort()
                            val sR = (sin(phaseR) * vol * 0.6 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i * 2] = sL
                            buffer[i * 2 + 1] = sR

                            phaseL += 2.0 * PI * freqL / sampleRate
                            if (phaseL >= 2.0 * PI) phaseL -= 2.0 * PI

                            phaseR += 2.0 * PI * freqR / sampleRate
                            if (phaseR >= 2.0 * PI) phaseR -= 2.0 * PI
                        }
                    }

                    SoundscapeType.RAIN_SHOWER -> {
                        for (i in 0 until chunkSamples) {
                            // Filtered gentle rainfall with random water drop clicks
                            val white = (random.nextDouble() * 2.0 - 1.0)
                            lastBrownL = (lastBrownL + (0.04 * white)) / 1.04
                            var drop = 0.0
                            if (rainDropCounter-- <= 0) {
                                drop = (random.nextDouble() - 0.5) * 0.8
                                rainDropCounter = random.nextInt(350) + 100
                            }
                            val s = ((lastBrownL * 1.5 + drop) * vol * 0.5 * Short.MAX_VALUE)
                                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                            buffer[i * 2] = s
                            buffer[i * 2 + 1] = s
                        }
                    }

                    SoundscapeType.ZEN_BOWL -> {
                        val bowlFreq = 432.0 // Verdi 432Hz tuning
                        for (i in 0 until chunkSamples) {
                            // Sub-harmonic drone with 0.2Hz gentle amplitude breath
                            val breath = 0.85 + 0.15 * sin(phaseL * 0.002)
                            val s = (sin(phaseL) * breath * vol * 0.5 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i * 2] = s
                            buffer[i * 2 + 1] = s

                            phaseL += 2.0 * PI * bowlFreq / sampleRate
                            if (phaseL >= 2.0 * PI) phaseL -= 2.0 * PI
                        }
                    }

                    SoundscapeType.FIRE_HEARTH -> {
                        for (i in 0 until chunkSamples) {
                            val white = (random.nextDouble() * 2.0 - 1.0)
                            lastBrownL = (lastBrownL + (0.015 * white)) / 1.015
                            var crackle = 0.0
                            if (crackleCounter-- <= 0) {
                                crackle = (random.nextDouble() - 0.3) * 0.95
                                crackleCounter = random.nextInt(900) + 200
                            }
                            val s = ((lastBrownL * 2.0 + crackle) * vol * 0.45 * Short.MAX_VALUE)
                                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                            buffer[i * 2] = s
                            buffer[i * 2 + 1] = s
                        }
                    }

                    else -> break
                }

                val written = audioTrack?.write(buffer, 0, buffer.size) ?: 0
                if (written < 0) break
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
        } catch (_: Exception) {}
        audioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
