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
import kotlin.random.Random

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
     * 播放任务完成提示音（叮叮声，约3秒）
     * 生成两音交替的悦耳门铃声
     */
    suspend fun playBeep() = withContext(Dispatchers.IO) {
        try {
            val durationMs = 3000
            val sampleCount = SAMPLE_RATE * durationMs / 1000
            val samples = ShortArray(sampleCount)

            // 两音交替：880Hz (A5) 和 1100Hz (C#6)
            val freq1 = 880.0
            val freq2 = 1100.0
            val cycleMs = 400 // 每个叮的周期（毫秒）
            val samplesPerCycle = SAMPLE_RATE * cycleMs / 1000

            for (i in samples.indices) {
                val posInCycle = i % samplesPerCycle
                val cycleStart = i - posInCycle
                val freq = if ((cycleStart / samplesPerCycle) % 2 == 0) freq1 else freq2
                val t = i.toDouble() / SAMPLE_RATE

                // 正弦波 + 包络（每周期渐强渐弱）
                val envPos = posInCycle.toDouble() / samplesPerCycle
                val envelope = sin(PI * envPos) // 0→1→0 的包络
                val value = (sin(2.0 * PI * freq * t) * envelope * 0.8).coerceIn(-1.0, 1.0)
                samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
            }

            playSamples(samples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 播放所有任务完成音效（拍手声，约6秒）
     * 生成白噪声 + 随机爆发模拟拍手
     */
    suspend fun playCongratulations() = withContext(Dispatchers.IO) {
        try {
            val durationMs = 6000
            val sampleCount = SAMPLE_RATE * durationMs / 1000
            val samples = ShortArray(sampleCount)

            val random = Random
            // 拍手爆发参数
            val clapInterval = 120  // 两次拍手之间的最小间隔（样本数）
            var nextClap = 0

            // 生成噪声 + 拍手爆发
            for (i in samples.indices) {
                val noise = random.nextFloat() * 2f - 1f  // -1 ~ 1 白噪声
                val clap: Float

                if (i >= nextClap) {
                    // 拍手爆发
                    val burstDuration = (random.nextInt(15) + 5) * 100  // 500~2000样本
                    val amplitude = random.nextFloat() * 0.3f + 0.7f  // 0.7~1.0
                    nextClap = i + burstDuration + random.nextInt(clapInterval)

                    // 爆发段：噪声以指数衰减模拟拍手
                    val decay = Math.exp(-4.0 * (i % burstDuration).toDouble() / burstDuration)
                    clap = (noise * amplitude * decay).toFloat()
                } else {
                    // 背景轻微噪声 + 远处的拍手
                    val distClap = noise * 0.15f
                    // 缓慢的全局起伏
                    val wave = sin(i.toDouble() / SAMPLE_RATE * 4.0 * PI).toFloat() * 0.1f
                    clap = distClap + wave
                }

                samples[i] = (clap.coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
            }

            playSamples(samples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通过 AudioTrack 播放 PCM 样本
     * 使用 STREAM_ALARM 确保高音量
     */
    private fun playSamples(samples: ShortArray) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0
        // 如果音量太低，临时提高
        if (currentVol < maxVol / 2) {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
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
