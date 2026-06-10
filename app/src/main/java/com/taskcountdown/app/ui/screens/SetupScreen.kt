package com.taskcountdown.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taskcountdown.app.model.Task
import com.taskcountdown.app.viewmodel.TaskViewModel

/**
 * 任务设置画面
 * 用户可以在此设定任务名称和倒计时时长
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: TaskViewModel,
    onStart: () -> Unit
) {
    val tasks = viewModel.tasks
    var showEmptyWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⏱ 任务设定",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "请输入任务名称并设定倒计时时间",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (showEmptyWarning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠ 请至少设定一个有效的任务（填写名称和时间）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = tasks,
                    key = { _, task -> task.id }
                ) { index, task ->
                    TaskInputCard(
                        index = index,
                        task = task,
                        onNameChange = { name -> viewModel.updateTaskName(index, name) },
                        onTimeChange = { mins, secs -> viewModel.updateTaskTime(index, mins, secs) },
                        onRemove = { viewModel.removeTask(index) },
                        canRemove = tasks.size > 1
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.addTask() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加任务",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加任务")
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        val validTasks = viewModel.getValidTasks()
                        if (validTasks.isEmpty()) {
                            showEmptyWarning = true
                        } else {
                            showEmptyWarning = false
                            val started = viewModel.startCountdown()
                            if (started) onStart()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "▶ 开始运行",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskInputCard(
    index: Int,
    task: Task,
    onNameChange: (String) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    val currentMinutes = (task.totalSeconds / 60).toInt().coerceIn(0, 999)
    val currentSeconds = (task.totalSeconds % 60).toInt().coerceIn(0, 59)

    var minutesText by remember(task.id) { mutableStateOf(currentMinutes.toString()) }
    var secondsText by remember(task.id) { mutableStateOf(currentSeconds.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = task.name,
                    onValueChange = { onNameChange(it) },
                    label = { Text("任务名称") },
                    placeholder = { Text("例如：做数学题") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除任务",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "倒计时:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { value ->
                        val filtered = value.filter { it.isDigit() }.take(3)
                        minutesText = filtered
                        val mins = filtered.toIntOrNull() ?: 0
                        val secs = secondsText.toIntOrNull() ?: 0
                        onTimeChange(mins.coerceIn(0, 999), secs.coerceIn(0, 59))
                    },
                    label = { Text("分") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    supportingText = { Text("分", style = MaterialTheme.typography.labelSmall) }
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { value ->
                        val filtered = value.filter { it.isDigit() }.take(2)
                        secondsText = filtered
                        val secs = filtered.toIntOrNull() ?: 0
                        val mins = minutesText.toIntOrNull() ?: 0
                        onTimeChange(mins.coerceIn(0, 999), secs.coerceIn(0, 59))
                    },
                    label = { Text("秒") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    supportingText = { Text("秒", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
