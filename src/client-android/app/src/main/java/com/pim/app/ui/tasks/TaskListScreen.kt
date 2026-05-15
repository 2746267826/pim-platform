package com.pim.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskListViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务管理") },
                actions = {
                    TextButton(onClick = viewModel::toggleInbox) {
                        Text(if (state.showInboxOnly) "全部" else "收件箱")
                    }
                    IconButton(onClick = viewModel::openCreateEditor) {
                        Icon(Icons.Default.Add, contentDescription = "新建任务")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.tasks.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                items(state.tasks) { task ->
                    val priorityColor = when (task.priority) {
                        1 -> Color(0xFFF59E0B)
                        2 -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = task.title, fontSize = 14.sp)
                                Text(
                                    text = "状态: ${task.status}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = task.due?.take(10) ?: "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // Task editor dialog
        if (state.isEditorOpen) {
            AlertDialog(
                onDismissRequest = viewModel::closeEditor,
                title = { Text("新建任务") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = state.editorTitle,
                            onValueChange = viewModel::updateEditorTitle,
                            label = { Text("标题") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.editorDescription,
                            onValueChange = viewModel::updateEditorDescription,
                            label = { Text("描述") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("优先级", fontSize = 12.sp)
                        Row {
                            listOf("普通" to 0, "重要" to 1, "紧急" to 2).forEach { (label, value) ->
                                FilterChip(
                                    selected = state.editorPriority == value,
                                    onClick = { viewModel.updateEditorPriority(value) },
                                    label = { Text(label) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::saveTask) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::closeEditor) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
