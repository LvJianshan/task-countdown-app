package com.taskcountdown.app.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskcountdown.app.model.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 应用状态枚举
 */
enum class AppState {
    SETUP,          // 设置画面
    COUNTDOWN,      // 倒计时画面
    COMPLETED       // 恭喜完成画面
}

/**
 * 拍照阶段
 */
enum class PhotoPhase {
    MIDPOINT,   // 任务进行到一半时拍照
    END         // 任务结束时拍照
}

/**
 * 拍照事件（ViewModel 发送给 UI 层）
 */
data class CaptureEvent(
    val taskIndex: Int,
    val taskName: String,
    val phase: PhotoPhase
)

/**
 * 照片记录（保存到 ViewModel 供恭喜画面展示）
 */
data class PhotoRecord(
    val taskIndex: Int,
    val taskName: String,
    val filePath: String,
    val phase: PhotoPhase
)

/**
 * 任务倒计时ViewModel - 管理所有业务逻辑
 */
class TaskViewModel : ViewModel() {

    // ==================== 状态 ====================

    /** 当前应用状态 */
    var appState by mutableStateOf(AppState.SETUP)
        private set

    /** 任务列表 */
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> get() = _tasks

    /** 当前正在倒计时的任务索引 */
    var currentTaskIndex by mutableIntStateOf(0)
        private set

    /** 当前任务剩余秒数 */
    var remainingSeconds by mutableLongStateOf(0L)
        private set

    /** 总任务数（仅计算有效任务） */
    var totalValidTasks by mutableIntStateOf(0)
        private set

    /** 是否在倒计时运行中 */
    var isRunning by mutableStateOf(false)
        private set

    /** 倒计时协程任务 */
    private var timerJob: Job? = null

    /** 标记是否需要播放音效 */
    var shouldPlayBeep by mutableStateOf(false)
        private set

    /** 标记是否需要播放完成音效 */
    var shouldPlayCongratulations by mutableStateOf(false)
        private set

    // ==================== 拍照相关 ====================

    /** 拍照事件流（ViewModel → UI，UI 层收到后执行拍照） */
    private val _captureEvent = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 64)
    val captureEvent: SharedFlow<CaptureEvent> = _captureEvent.asSharedFlow()

    /** 已拍摄的照片记录 */
    private val _capturedPhotos = mutableStateListOf<PhotoRecord>()
    val capturedPhotos: List<PhotoRecord> get() = _capturedPhotos

    /** 当前任务是否已拍过中点照 */
    private var hasTakenMidpointPhoto = false

    init {
        // 默认初始化5个任务
        resetToDefaults()
    }

    /**
     * 重置为默认5个空任务
     */
    fun resetToDefaults() {
        _tasks.clear()
        for (i in 0 until 5) {
            _tasks.add(Task(id = i, name = "", totalSeconds = 60L))
        }
        appState = AppState.SETUP
        currentTaskIndex = 0
        remainingSeconds = 0L
        isRunning = false
        shouldPlayBeep = false
        shouldPlayCongratulations = false
        _capturedPhotos.clear()
        hasTakenMidpointPhoto = false
    }

    /**
     * 添加一个新的空任务
     */
    fun addTask() {
        val newId = _tasks.size
        _tasks.add(Task(id = newId, name = "", totalSeconds = 60L))
    }

    /**
     * 更新指定任务名称
     */
    fun updateTaskName(index: Int, name: String) {
        if (index in _tasks.indices) {
            _tasks[index] = _tasks[index].copy(name = name)
        }
    }

    /**
     * 更新指定任务的倒计时时长（秒）
     */
    fun updateTaskTime(index: Int, minutes: Int, seconds: Int) {
        if (index in _tasks.indices) {
            val totalSec = (minutes * 60L + seconds).coerceAtLeast(1L)
            _tasks[index] = _tasks[index].copy(totalSeconds = totalSec)
        }
    }

    /**
     * 删除指定任务
     */
    fun removeTask(index: Int) {
        if (_tasks.size > 1 && index in _tasks.indices) {
            _tasks.removeAt(index)
        }
    }

    /**
     * 获取过滤后的有效任务列表
     */
    fun getValidTasks(): List<Task> = _tasks.filter { it.isValid() }

    /**
     * 开始倒计时
     * @return true 表示成功启动，false 表示没有有效任务
     */
    fun startCountdown(): Boolean {
        val validTasks = getValidTasks()
        if (validTasks.isEmpty()) return false

        totalValidTasks = validTasks.size

        // 只保留有效任务
        _tasks.clear()
        _tasks.addAll(validTasks)
        // 重新分配id以保证连续性
        for (i in _tasks.indices) {
            _tasks[i] = _tasks[i].copy(id = i)
        }

        currentTaskIndex = 0
        remainingSeconds = _tasks[0].totalSeconds
        appState = AppState.COUNTDOWN
        isRunning = true
        _capturedPhotos.clear()
        hasTakenMidpointPhoto = false
        startTimer()
        return true
    }

    /**
     * 启动倒计时协程
     * 每次循环后检查中点/结束拍照条件，发送事件到 UI 层
     */
    private fun startTimer() {
        timerJob?.cancel()
        val currentTask = _tasks.getOrNull(currentTaskIndex)
        val taskName = currentTask?.name ?: ""
        hasTakenMidpointPhoto = false
        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0 && isRunning) {
                delay(1000L)
                remainingSeconds--

                // === 中点拍照触发（进度到 50% 时）===
                if (!hasTakenMidpointPhoto && isRunning) {
                    val task = _tasks.getOrNull(currentTaskIndex) ?: continue
                    val halfPoint = task.totalSeconds / 2
                    if (remainingSeconds == halfPoint && task.totalSeconds > 1) {
                        hasTakenMidpointPhoto = true
                        _captureEvent.trySend(
                            CaptureEvent(currentTaskIndex, task.name, PhotoPhase.MIDPOINT)
                        )
                    }
                }
            }
            // 倒计时结束，触发音效
            if (isRunning) {
                // === 结束拍照触发（任务完成瞬间）===
                val endedTask = _tasks.getOrNull(currentTaskIndex)
                if (endedTask != null) {
                    _captureEvent.trySend(
                        CaptureEvent(currentTaskIndex, endedTask.name, PhotoPhase.END)
                    )
                }

                shouldPlayBeep = true
                delay(100L) // 短暂等待音效触发
                // 进入下一个任务
                if (currentTaskIndex < totalValidTasks - 1) {
                    currentTaskIndex++
                    remainingSeconds = _tasks[currentTaskIndex].totalSeconds
                    startTimer()
                } else {
                    // 所有任务完成
                    isRunning = false
                    shouldPlayCongratulations = true
                    appState = AppState.COMPLETED
                }
            }
        }
    }

    /**
     * 标记音效已处理
     */
    fun onBeepHandled() {
        shouldPlayBeep = false
    }

    /**
     * 标记完成音效已处理
     */
    fun onCongratulationsHandled() {
        shouldPlayCongratulations = false
    }

    /**
     * 添加拍摄的照片记录（由 UI 层在拍照完成后调用）
     */
    fun addCapturedPhoto(record: PhotoRecord) {
        _capturedPhotos.add(record)
    }

    /**
     * 停止倒计时
     */
    fun stopCountdown() {
        isRunning = false
        timerJob?.cancel()
        appState = AppState.SETUP
    }

    /**
     * 重新开始
     */
    fun restart() {
        stopCountdown()
        resetToDefaults()
    }

    /**
     * 格式化时间为 MM:SS
     */
    fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
