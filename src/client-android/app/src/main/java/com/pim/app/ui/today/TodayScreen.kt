package com.pim.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        PimSection("今日轨迹") {
            Text("轨迹预览、停留点和移动线将在这里展示。")
            Text("采集状态：待配置")
        }
        PimSection("位置指标") {
            Text("停留：0")
            Text("移动距离：0 km")
            Text("质量完整度：等待定位")
        }
        PimSection("手机使用") {
            Text("前台使用时长：等待同步")
            Text("Top App：暂无数据")
        }
        PimSection("当前策略") {
            Text("省电档，常规间隔 3 分钟")
            Text("下次定位：等待持续采集开启")
        }
    }
}
