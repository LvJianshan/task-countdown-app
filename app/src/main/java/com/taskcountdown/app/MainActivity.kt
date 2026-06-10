package com.taskcountdown.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskcountdown.app.ui.screens.CongratulationsScreen
import com.taskcountdown.app.ui.screens.CountdownScreen
import com.taskcountdown.app.ui.screens.SetupScreen
import com.taskcountdown.app.ui.theme.TaskCountdownTheme
import com.taskcountdown.app.util.SoundManager
import com.taskcountdown.app.viewmodel.AppState
import com.taskcountdown.app.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaskCountdownTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskCountdownApp()
                }
            }
        }
    }
}

/**
 * 应用主入口 Composable
 * 根据 ViewModel 中的状态切换不同画面
 */
@Composable
fun TaskCountdownApp(
    viewModel: TaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val appState = viewModel.appState

    // 处理倒计时结束音效
    LaunchedEffect(viewModel.shouldPlayBeep) {
        if (viewModel.shouldPlayBeep) {
            soundManager.playBeep()
            viewModel.onBeepHandled()
        }
    }

    // 处理恭喜完成音效
    LaunchedEffect(viewModel.shouldPlayCongratulations) {
        if (viewModel.shouldPlayCongratulations) {
            soundManager.playCongratulations(
                scope = this,  // LaunchedEffect本身就是一个CoroutineScope
                onComplete = { viewModel.onCongratulationsHandled() }
            )
        }
    }

    // 画面切换动画
    AnimatedContent(
        targetState = appState,
        transitionSpec = {
            when (targetState) {
                AppState.SETUP -> {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                }
                AppState.COUNTDOWN -> {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                }
                AppState.COMPLETED -> {
                    scaleIn(
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400)) togetherWith
                            scaleOut(
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                }
            }
        },
        label = "screenTransition"
    ) { state ->
        when (state) {
            AppState.SETUP -> {
                SetupScreen(
                    viewModel = viewModel,
                    onStart = { /* 无需额外操作，ViewModel已处理状态切换 */ }
                )
            }
            AppState.COUNTDOWN -> {
                CountdownScreen(
                    viewModel = viewModel,
                    onFinish = { /* 停止时回到设置画面，ViewModel已处理 */ }
                )
            }
            AppState.COMPLETED -> {
                CongratulationsScreen(
                    viewModel = viewModel,
                    onRestart = { /* 重新开始回到设置画面 */ }
                )
            }
        }
    }
}
