package com.pim.app.ui.status

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
fun StatusCenterScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("状态中心", style = MaterialTheme.typography.headlineSmall)
        PimSection("API") {
            Text("API 地址：未配置")
            Text("登录状态：未登录")
            Text("最近错误：无")
        }
        PimSection("权限") {
            Text("通知、前台定位、后台定位、使用情况和活动识别权限会在这里展示。")
        }
        PimSection("前台服务") {
            Text("持续采集：未开启")
            Text("当前策略：省电档")
            Text("下次定位：等待开启")
        }
        PimSection("上传队列") {
            Text("待上传定位：0")
            Text("心跳：等待同步")
            Text("同步尝试：暂无")
        }
        PimSection("最近日志") {
            Text("最近错误：无")
            Text("最近丢弃原因：无")
        }
    }
}
