package com.taskcountdown.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * 音效管理器
 * 使用 AudioTrack 直接生成 PCM 音频数据播放
 * 不依赖系统铃声（系统铃声在某些手机上可能静音或音量极低）
 * 使用 STREAM_ALARM 通道，音量独立于媒体/通知音量
 */
class SoundManager(private val context: Context) {

    companion object {
        private const val SAMPLE_RATE = 44100
    }

    /**
     * 播放任务完成提示音（轻柔风铃声，约3秒）
     * 低频正弦波 + 缓慢包络，柔和舒缓
     */
    suspend fun playBeep() = withContext(Dispatchers.IO) {
        try {
            val durationMs = 3000
            val sampleCount = SAMPLE_RATE * durationMs / 1000
            val samples = ShortArray(sampleCount)

            // 柔和的风铃音：两个低频泛音叠加，缓慢衰减
            val freq1 = 392.0  // G4
            val freq2 = 523.0  // C5
            val freq3 = 659.0  // E5

            for (i in samples.indices) {
                val t = i.toDouble() / SAMPLE_RATE
                // 整体缓慢衰减包络（前1秒渐强，后2秒渐弱）
                val envelope: Double
                if (t < 1.0) {
                    envelope = sin(PI * t / 2.0)  // 0→1 缓慢渐强
                } else {
                    envelope = Math.exp(-1.5 * (t - 1.0))  // 指数衰减
                }
                // 三个泛音叠加，降低音量
                val value1 = sin(2.0 * PI * freq1 * t) * 0.35
                val value2 = sin(2.0 * PI * freq2 * t) * 0.25
                val value3 = sin(2.0 * PI * freq3 * t) * 0.15
                val value = (value1 + value2 + value3) * envelope * 0.6
                samples[i] = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }

            playSamples(samples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 播放所有任务完成音效（舒缓上行琶音，约6秒）
     * 类似音乐盒的轻柔旋律
     */
    suspend fun playCongratulations() = withContext(Dispatchers.IO) {
        try {
            val durationMs = 6000
            val sampleCount = SAMPLE_RATE * durationMs / 1000
            val samples = ShortArray(sampleCount)

            // 舒缓的琶音序列：C4 → E4 → G4 → C5 → E5 → G5
            val notes = listOf(
                262.0,  // C4
                330.0,  // E4
                392.0,  // G4
                523.0,  // C5
                659.0,  // E5
                784.0   // G5
            )
            val noteDuration = durationMs / notes.size  // 每个音持续时间（毫秒）

            for (i in samples.indices) {
                val t = i.toDouble() / SAMPLE_RATE
                val noteIndex = (i.toLong() * notes.size / sampleCount).toInt().coerceIn(0, notes.size - 1)
                val noteStartMs = noteIndex * noteDuration
                val noteTime = t - noteStartMs / 1000.0

                // 每个音的包络：渐强→持续→渐弱
                val noteDurSec = noteDuration / 1000.0
                val envelope = when {
                    noteTime < 0.2 -> noteTime / 0.2  // 0→1 渐强
                    noteTime > noteDurSec - 0.3 -> ((noteDurSec - noteTime) / 0.3).coerceIn(0.0, 1.0)  // 渐弱
                    else -> 1.0
                }

                val freq = notes[noteIndex]
                // 基音 + 柔和泛音
                val value = (sin(2.0 * PI * freq * t) * 0.4
                    + sin(2.0 * PI * freq * 2 * t) * 0.1
                    + sin(2.0 * PI * freq * 3 * t) * 0.05) * envelope * 0.5

                samples[i] = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }

            playSamples(samples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通过 AudioTrack 播放 PCM 样本
     * 使用 STREAM_NOTIFICATION 跟随系统铃声/通知音量
     * 系统静音或震动模式下不发声
     */
    private fun playSamples(samples: ShortArray) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // 检查系统铃声模式：静音或震动时不播放
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            return
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)  // 每个 sample 2 bytes
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()

        // 等待播放完成
        val durationMs = (samples.size.toLong() * 1000) / SAMPLE_RATE
        Thread.sleep(durationMs + 200)

        track.stop()
        track.release()
    }
}
