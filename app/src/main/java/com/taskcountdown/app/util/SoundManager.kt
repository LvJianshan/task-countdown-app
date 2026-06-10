package com.taskcountdown.app.util

import android.content.Context
import android.media.RingtoneManager
import android.media.Ringtone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 音效管理器 - 负责播放倒计时结束提示音和恭喜音效
 */
class SoundManager(private val context: Context) {

    /**
     * 播放单次提示音（任务完成时的响铃）
     */
    fun playBeep() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 播放连续5次祝贺音
     * @param scope 协程作用域
     * @param onComplete 播放完成后的回调
     */
    fun playCongratulations(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                for (i in 0 until 5) {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.play()
                    // 每次响铃之间间隔600ms
                    delay(600L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onComplete()
        }
    }
}
