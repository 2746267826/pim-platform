package com.pim.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("全局搜索") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            // Search bar
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                label = { Text("搜索...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = viewModel::search) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            Row {
                FilterChip(
                    selected = state.typeFilter == null,
                    onClick = { viewModel.filterByType(null) },
                    label = { Text("全部") },
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = state.typeFilter == "event",
                    onClick = { viewModel.filterByType("event") },
                    label = { Text("事件") },
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = state.typeFilter == "task",
                    onClick = { viewModel.filterByType("task") },
                    label = { Text("任务") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            // Results
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.results) { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (result.type == "event") Color(0xFFDBEAFE)
                                            else Color(0xFFFEF3C7),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = result.type,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = result.snippet,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
