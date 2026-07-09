package com.pim.app.ui.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pim.app.ui.components.PimSection

@Composable
fun TracksScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("轨迹历史", style = MaterialTheme.typography.headlineSmall)
        PimSection("时间范围") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("今日") })
                AssistChip(onClick = {}, label = { Text("7 天") })
                AssistChip(onClick = {}, label = { Text("30 天") })
            }
        }
        PimSection("质量过滤") {
            Text("默认仅显示 < 50m 的定位点。")
            Text("可在状态中心查看被丢弃的低质量点。")
        }
        PimSection("轨迹片段") {
            Text("移动、停留、缺口和低置信片段将在这里展示。")
        }
        PimSection("片段详情") {
            Text("选择片段后展示时长、距离、平均速度、平均精度和质量标记。")
        }
        PimSection("原始点") {
            Text("选择片段后显示对应原始定位点。")
        }
    }
}
