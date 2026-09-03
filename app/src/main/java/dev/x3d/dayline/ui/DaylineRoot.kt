package dev.x3d.dayline.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.ui.lesson.LessonScreen
import dev.x3d.dayline.ui.login.LoginScreen
import dev.x3d.dayline.ui.login.QrScanScreen
import dev.x3d.dayline.ui.school.SchoolScreen
import dev.x3d.dayline.ui.settings.SettingsScreen
import dev.x3d.dayline.ui.today.TodayScreen
import dev.x3d.dayline.ui.week.WeekScreen
import org.koin.compose.koinInject

object Routes {
    const val SCHOOL = "school"
    const val LOGIN = "login"
    const val QR = "qr"
    const val TODAY = "today"
    const val WEEK = "week"
    const val SETTINGS = "settings"
    const val LESSON = "lesson/{id}"
    fun lesson(id: Long) = "lesson/$id"
}

@Composable
fun DaylineRoot(repository: PeriodRepository = koinInject()) {
    val nav = rememberNavController()
    val start = if (repository.session.value == null) Routes.SCHOOL else Routes.TODAY
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Routes.TODAY, Routes.WEEK, Routes.SETTINGS)
    val showBar = route in tabs

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.TODAY,
                        onClick = { nav.tab(Routes.TODAY) },
                        icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_today)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.WEEK,
                        onClick = { nav.tab(Routes.WEEK) },
                        icon = { Icon(Icons.Outlined.CalendarViewWeek, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_week)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.SETTINGS,
                        onClick = { nav.tab(Routes.SETTINGS) },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SCHOOL) {
                SchoolScreen(onContinue = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.SCHOOL) { inclusive = false } } })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        nav.navigate(Routes.TODAY) {
                            popUpTo(Routes.SCHOOL) { inclusive = true }
                        }
                    },
                    onScanQr = { nav.navigate(Routes.QR) },
                    onChangeSchool = { nav.popBackStack() },
                )
            }
            composable(Routes.QR) {
                QrScanScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.TODAY) {
                TodayScreen(onLesson = { nav.navigate(Routes.lesson(it)) })
            }
            composable(Routes.WEEK) {
                WeekScreen(onLesson = { nav.navigate(Routes.lesson(it)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLoggedOut = {
                        nav.navigate(Routes.SCHOOL) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Routes.LESSON,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                LessonScreen(id = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavHostController.tab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
