package com.taskcountdown.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskcountdown.app.ui.theme.RedWarning
import com.taskcountdown.app.viewmodel.TaskViewModel

/**
 * 全屏倒计时画面
 * 显示当前任务名称和大号倒计时数字
 * 最后一分钟时数字变为红色
 */
@Composable
fun CountdownScreen(
    viewModel: TaskViewModel,
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    val task = if (viewModel.currentTaskIndex < viewModel.tasks.size)
        viewModel.tasks[viewModel.currentTaskIndex] else null

    // 保持屏幕常亮
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 动画：数字变化时的脉冲效果
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // 检测倒计时结束切换到下一个任务
    val isLastMinute = viewModel.remainingSeconds <= 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 任务进度指示
            Text(
                text = "任务 ${viewModel.currentTaskIndex + 1} / ${viewModel.totalValidTasks}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 当前任务名称
            Text(
                text = task?.name ?: "未知任务",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 大号倒计时数字
            Text(
                text = viewModel.formatTime(viewModel.remainingSeconds),
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLastMinute) RedWarning else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isLastMinute) pulseAlpha else 1f)
            )

            // "最后一分钟"提示
            AnimatedVisibility(visible = isLastMinute && viewModel.remainingSeconds > 0) {
                Text(
                    text = "⏰ 最后一分钟！",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RedWarning,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 进度条
            if (task != null && task.totalSeconds > 0) {
                val progress = viewModel.remainingSeconds.toFloat() / task.totalSeconds.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (isLastMinute) RedWarning else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 停止按钮
            OutlinedButton(
                onClick = {
                    viewModel.stopCountdown()
                    onFinish()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            ) {
                Text("停止")
            }
        }
    }
}
