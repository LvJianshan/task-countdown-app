package com.taskcountdown.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskcountdown.app.ui.theme.GoldYellow
import com.taskcountdown.app.viewmodel.PhotoPhase
import com.taskcountdown.app.viewmodel.PhotoRecord
import com.taskcountdown.app.viewmodel.TaskViewModel

/**
 * 恭喜完成画面
 * 所有任务完成后显示庆祝界面
 */
@Composable
fun CongratulationsScreen(
    viewModel: TaskViewModel,
    onRestart: () -> Unit = {}
) {
    // 动画：星星旋转和缩放
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )
    val starRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 庆祝动画星星
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = GoldYellow,
                modifier = Modifier
                    .size(120.dp)
                    .scale(starScale)
                    .rotate(starRotation)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 恭喜标题
            Text(
                text = "🎉 恭喜！🎉",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = GoldYellow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 完成信息
            Text(
                text = "所有任务已完成！",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "你完成了 ${viewModel.totalValidTasks} 个任务",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // ===== 照片画廊 =====
            if (viewModel.capturedPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "📸 拍摄快照",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    items(viewModel.capturedPhotos, key = { it.filePath }) { photo ->
                        PhotoThumbnailCard(photo = photo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 任务列表回顾
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "任务回顾",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    viewModel.tasks.forEachIndexed { index, task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = task.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = viewModel.formatTime(task.totalSeconds),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 重新开始按钮
            Button(
                onClick = {
                    viewModel.restart()
                    onRestart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "返回设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 照片缩略图卡片
 */
@Composable
private fun PhotoThumbnailCard(photo: PhotoRecord) {
    val bitmap = remember(photo.filePath) {
        loadPhotoThumbnail(photo.filePath, maxSize = 240)
    }

    Card(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "照片：${photo.taskName} ${if (photo.phase == PhotoPhase.MIDPOINT) "中途" else "结束"}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 加载失败时的占位
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 32.sp)
                }
            }

            // 底部标签：任务名 + 阶段
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
            ) {
                Text(
                    text = "${photo.taskName}\n${if (photo.phase == PhotoPhase.MIDPOINT) "⏳ 中途" else "✅ 结束"}",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * 加载照片缩略图（减小内存占用）
 */
private fun loadPhotoThumbnail(path: String, maxSize: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        val sampleSize = minOf(
            options.outWidth / maxSize,
            options.outHeight / maxSize,
            Int.MAX_VALUE
        ).coerceAtLeast(1)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        BitmapFactory.decodeFile(path, decodeOptions)
    } catch (e: Exception) {
        null
    }
}
