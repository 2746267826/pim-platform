package com.pim.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历") },
                actions = {
                    IconButton(onClick = { /* sync outlook */ }) {
                        Icon(Icons.Default.Sync, contentDescription = "同步")
                    }
                    IconButton(onClick = { viewModel.openCreateEditor() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建事件")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                }
                Text(
                    text = state.currentMonth.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月")
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                }
            }

            // Day headers
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                val days = listOf("一", "二", "三", "四", "五", "六", "日")
                for (day in days) {
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar grid
            val month = state.currentMonth
            val firstDay = month.atDay(1).dayOfWeek.value // 1=Mon, 7=Sun
            val leadingBlanks = firstDay - 1
            val daysInMonth = month.lengthOfMonth()
            val totalCells = leadingBlanks + daysInMonth

            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    for (col in 0..6) {
                        val cellIdx = row * 7 + col
                        val day = cellIdx - leadingBlanks + 1

                        if (day in 1..daysInMonth) {
                            val dayEvents = viewModel.getEventsForDay(day)
                            val isToday = month.atDay(day) == LocalDate.now()

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.openCreateEditor(month.atDay(day))
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 12.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (dayEvents.isNotEmpty()) {
                                    Row {
                                        repeat(minOf(dayEvents.size, 3)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .padding(0.5.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            // Events list
            if (state.events.isNotEmpty()) {
                Text(
                    text = "事件列表",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
                ) {
                    items(state.events) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { viewModel.openEditEditor(event) },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = event.title, fontSize = 13.sp)
                                    Text(
                                        text = event.dtStart.take(16),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Event editor dialog
        if (state.isEditorOpen) {
            AlertDialog(
                onDismissRequest = viewModel::closeEditor,
                title = { Text(if (state.selectedEvent != null) "编辑事件" else "新建事件") },
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
                            value = state.editorLocation,
                            onValueChange = viewModel::updateEditorLocation,
                            label = { Text("地点") },
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
                        OutlinedTextField(
                            value = state.editorStart,
                            onValueChange = viewModel::updateEditorStart,
                            label = { Text("开始时间") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.editorEnd,
                            onValueChange = viewModel::updateEditorEnd,
                            label = { Text("结束时间") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::saveEvent) {
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
