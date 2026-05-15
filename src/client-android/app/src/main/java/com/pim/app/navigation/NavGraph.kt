package com.pim.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pim.app.ui.calendar.CalendarScreen
import com.pim.app.ui.calendar.CalendarViewModel
import com.pim.app.ui.login.LoginScreen
import com.pim.app.ui.login.LoginViewModel
import com.pim.app.ui.search.SearchScreen
import com.pim.app.ui.search.SearchViewModel
import com.pim.app.ui.tasks.TaskListScreen
import com.pim.app.ui.tasks.TaskListViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    object Calendar : Screen("calendar", "日历", Icons.Default.CalendarMonth)
    object Tasks : Screen("tasks", "任务", Icons.Default.ListAlt)
    object Search : Screen("search", "搜索", Icons.Default.Search)
}

val bottomNavItems = listOf(Screen.Calendar, Screen.Tasks, Screen.Search)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        val loginVm: LoginViewModel = hiltViewModel()
        LoginScreen(
            viewModel = loginVm,
            onLoginSuccess = { isLoggedIn = true }
        )
    } else {
        MainScaffold(navController = navController)
    }
}

@Composable
fun MainScaffold(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Calendar.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Tasks.route) {
                TaskListScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Search.route) {
                SearchScreen(viewModel = hiltViewModel())
            }
        }
    }
}
