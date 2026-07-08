package com.pim.app.ui.settings

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
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        PimSection("API 地址") {
            Text("示例：https://pim.example.com/api/v1/")
            Text("支持公网 IP 或域名。")
        }
        PimSection("账号") {
            Text("登录后才可以同步和上传。")
        }
        PimSection("持续采集") {
            Text("默认关闭，需要手动开启。")
        }
        PimSection("省电档") {
            Text("常规间隔：3 分钟")
            Text("日程低频：15 分钟")
            Text("移动间隔：1 分钟")
            Text("上传精度：< 50m")
        }
    }
}
