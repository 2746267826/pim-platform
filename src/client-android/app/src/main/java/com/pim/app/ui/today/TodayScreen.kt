package com.pim.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
fun TodayScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("今日概览", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("持续采集：未开启") })
            AssistChip(onClick = {}, label = { Text("API：待连接") })
        }
        PimSection("今日轨迹") {
            Text("地图预览将在这里展示今日轨迹、停留点和移动方向。")
            Text("最近位置：暂无符合 < 50m 精度的定位。")
        }
        PimSection("位置指标") {
            Text("停留：0 次")
            Text("移动距离：0.0 km")
            Text("质量完整度：等待定位")
        }
        PimSection("手机使用") {
            Text("今日前台使用：等待同步")
            Text("Top App：暂无数据")
        }
        PimSection("当前策略") {
            Text("当前策略：省电档")
            Text("下次定位：持续采集开启后计算")
            Text("异常提示会出现在状态中心。")
        }
    }
}
