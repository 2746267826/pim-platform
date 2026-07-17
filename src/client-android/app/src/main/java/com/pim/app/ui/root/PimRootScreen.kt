package com.pim.app.ui.root

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pim.app.ui.schedule.SchedulePolicyScreen
import com.pim.app.ui.settings.SettingsScreen
import com.pim.app.ui.status.StatusCenterScreen
import com.pim.app.ui.theme.PimTheme
import com.pim.app.ui.today.TodayScreen
import com.pim.app.ui.tracks.TracksScreen

@Composable
fun PimRootScreen(initialDestination: PimDestination = PimDestination.Today) {
    var selected by rememberSaveable(initialDestination.name) { mutableStateOf(initialDestination) }
    var savedTracksUrl by rememberSaveable { mutableStateOf<String?>(null) }

    PimTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    PimDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { selected = destination },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (selected) {
                PimDestination.Today -> TodayScreen(modifier, onOpenSettings = { selected = PimDestination.Settings })
                PimDestination.Tracks -> TracksScreen(
                    modifier = modifier,
                    onOpenSettings = { selected = PimDestination.Settings },
                    savedUrl = savedTracksUrl,
                    onUrlChanged = { savedTracksUrl = it }
                )
                PimDestination.Schedule -> SchedulePolicyScreen(modifier)
                PimDestination.Status -> StatusCenterScreen(
                    modifier = modifier,
                    onOpenSettings = { selected = PimDestination.Settings }
                )
                PimDestination.Settings -> SettingsScreen(modifier)
            }
        }
    }
}
