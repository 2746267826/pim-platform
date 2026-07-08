package com.pim.app.ui.schedule

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
fun SchedulePolicyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("日程低频策略", style = MaterialTheme.typography.headlineSmall)
        PimSection("当前日程") {
            Text("当前没有带位置信息的日程。")
        }
        PimSection("策略影响") {
            Text("日程低频：15 分钟")
            Text("恢复阈值：100m")
        }
        PimSection("策略切换") {
            Text("进入日程低频、检测运动、移动超过阈值、恢复常规间隔的记录会显示在这里。")
        }
    }
}
