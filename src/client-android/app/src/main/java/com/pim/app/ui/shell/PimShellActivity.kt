package com.pim.app.ui.shell

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pim.app.daemon.DataCollector
import com.pim.app.ui.permissions.PermissionCenterScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PimShellActivity : AppCompatActivity() {
    @Inject lateinit var collector: DataCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collector.start()
        val initialRoute = intent.getStringExtra(EXTRA_ROUTE) ?: "/today"
        setContent {
            PimShellScreen(initialRoute = initialRoute)
        }
    }

    override fun onDestroy() {
        if (::collector.isInitialized) {
            collector.stop()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ROUTE = "com.pim.app.extra.ROUTE"

        fun intentFor(context: Context, route: String): Intent {
            return Intent(context, PimShellActivity::class.java).putExtra(EXTRA_ROUTE, route)
        }
    }
}

@Composable
fun PimShellScreen(initialRoute: String = "/today") {
    var route by rememberSaveable { mutableStateOf(initialRoute) }
    var showPermissions by rememberSaveable { mutableStateOf(false) }
    val routes = listOf(
        "今日" to "/today",
        "任务" to "/tasks",
        "日历" to "/calendar",
        "报告" to "/reports",
        "Outlook" to "/sync",
        "Data Center" to "/data-center",
        "确认" to "/confirmations"
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "PIM Android Companion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "权限中心、collection quality、上传队列与嵌入 Web 工作台",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { showPermissions = true }) {
                        Text("权限中心")
                    }
                    routes.forEach { (label, targetRoute) ->
                        Button(onClick = {
                            route = targetRoute
                            showPermissions = false
                        }) {
                            Text(label)
                        }
                    }
                }

                if (showPermissions) {
                    PermissionCenterScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        collectionQuality = "collection quality: good"
                    )
                } else {
                    PimWebViewScreen(
                        route = route,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(620.dp)
                            .weight(1f)
                    )
                }
            }
        }
    }
}
