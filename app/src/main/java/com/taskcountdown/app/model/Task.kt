package com.taskcountdown.app.model

/**
 * 任务数据类
 * @param id 唯一标识
 * @param name 任务名称
 * @param totalSeconds 设定的总倒计时秒数
 */
data class Task(
    val id: Int,
    val name: String = "",
    val totalSeconds: Long = 60L
) {
    /**
     * 判断任务是否有效（有名称且倒计时>0）
     */
    fun isValid(): Boolean = name.isNotBlank() && totalSeconds > 0
}
