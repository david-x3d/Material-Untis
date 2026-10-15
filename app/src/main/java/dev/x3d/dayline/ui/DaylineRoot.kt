package dev.x3d.dayline.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.ui.absences.AbsencesScreen
import dev.x3d.dayline.ui.lesson.LessonScreen
import dev.x3d.dayline.ui.login.LoginScreen
import dev.x3d.dayline.ui.login.QrScanScreen
import dev.x3d.dayline.ui.onboarding.AccountTypeScreen
import dev.x3d.dayline.ui.school.SchoolScreen
import dev.x3d.dayline.ui.settings.SettingsScreen
import dev.x3d.dayline.ui.timetable.TimetableScreen
import org.koin.core.context.GlobalContext

object Routes {
    const val WELCOME = "welcome"
    const val SCHOOL = "school"
    const val LOGIN = "login"
    const val QR = "qr"
    const val TIMETABLE = "timetable"
    const val ABSENCES = "absences"
    const val SETTINGS = "settings"
    const val LESSON = "lesson/{id}"
    fun lesson(id: Long) = "lesson/$id"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DaylineRoot() {
    val repository = remember { GlobalContext.get().get<PeriodRepository>() }
    val nav = rememberNavController()
    val start = if (repository.session.value == null) Routes.WELCOME else Routes.TIMETABLE
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(
        Tab(Routes.TIMETABLE, R.string.nav_timetable, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        Tab(Routes.ABSENCES, R.string.nav_absences, Icons.Outlined.Sick, Icons.Filled.Sick),
        Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val showBar = tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        ShortNavigationBarItem(
                            selected = selected,
                            onClick = { nav.tab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Routes.WELCOME) {
                AccountTypeScreen(onContinue = { nav.navigate(Routes.SCHOOL) })
            }
            composable(Routes.SCHOOL) {
                SchoolScreen(onContinue = { nav.navigate(Routes.LOGIN) })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        nav.navigate(Routes.TIMETABLE) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                    onScanQr = { nav.navigate(Routes.QR) },
                    onChangeSchool = { nav.popBackStack() },
                )
            }
            composable(Routes.QR) {
                QrScanScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.TIMETABLE) {
                TimetableScreen(onLesson = { nav.navigate(Routes.lesson(it)) })
            }
            composable(Routes.ABSENCES) {
                AbsencesScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLoggedOut = {
                        nav.navigate(Routes.WELCOME) {
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

private data class Tab(
    val route: String,
    val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)
