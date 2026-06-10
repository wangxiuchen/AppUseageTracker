package com.appspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appspy.ui.permission.PermissionScreen
import com.appspy.ui.theme.InkSub
import com.appspy.ui.theme.NavBg
import com.appspy.ui.theme.Primary
import com.appspy.ui.today.TodayScreen
import com.appspy.ui.trend.TrendScreen
import com.appspy.util.PermissionUtil

sealed class Screen(val route: String, val label: String) {
    data object Today : Screen("today", "今日")
    data object Trend : Screen("trend", "趋势")
}

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 每次 App 从后台回到前台（onResume）时重新检查权限
    var hasPermission by remember { mutableStateOf(PermissionUtil.hasUsageStatsPermission(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionUtil.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        PermissionScreen()
        return
    }

    val navController = rememberNavController()
    val items = listOf(Screen.Today, Screen.Trend)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NavBg,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Today -> BarChartIcon(if (selected) Primary else InkSub)
                                Screen.Trend -> LineChartIcon(if (selected) Primary else InkSub)
                            }
                        },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            unselectedIconColor = InkSub,
                            selectedTextColor = Primary,
                            unselectedTextColor = InkSub,
                            indicatorColor = NavBg
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route
        ) {
            composable(Screen.Today.route) {
                TodayScreen(contentPadding = innerPadding)
            }
            composable(Screen.Trend.route) {
                TrendScreen(contentPadding = innerPadding)
            }
        }
    }
}

/** 柱状图图标（今日 Tab） */
@Composable
private fun BarChartIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val bw = w * 0.22f
        val gap = w * 0.12f
        val x0 = (w - 3 * bw - 2 * gap) / 2
        drawRect(color, Offset(x0, h * 0.52f), Size(bw, h * 0.48f))
        drawRect(color, Offset(x0 + bw + gap, h * 0.25f), Size(bw, h * 0.75f))
        drawRect(color, Offset(x0 + 2 * (bw + gap), h * 0.04f), Size(bw, h * 0.96f))
    }
}

/** 折线图图标（趋势 Tab） */
@Composable
private fun LineChartIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 2.2f * density
        val path = Path().apply {
            moveTo(0f, h * 0.78f)
            lineTo(w * 0.28f, h * 0.42f)
            lineTo(w * 0.58f, h * 0.60f)
            lineTo(w, h * 0.10f)
        }
        drawPath(path, color, style = Stroke(width = strokePx))
    }
}
