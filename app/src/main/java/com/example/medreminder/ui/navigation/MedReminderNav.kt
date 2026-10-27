package com.example.medreminder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medreminder.MedReminderApplication
import com.example.medreminder.R
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.ui.calendar.CalendarScreen
import com.example.medreminder.ui.calendar.CalendarViewModel
import com.example.medreminder.ui.courses.CoursesScreen
import com.example.medreminder.ui.courses.CoursesViewModel
import com.example.medreminder.domain.model.CourseKind
import com.example.medreminder.ui.courseedit.CourseCreateTypeScreen
import com.example.medreminder.ui.courseedit.CourseEditScreen
import com.example.medreminder.ui.courseedit.CourseEditViewModel
import com.example.medreminder.ui.history.HistoryScreen
import com.example.medreminder.ui.history.HistoryViewModel
import com.example.medreminder.ui.settings.SettingsScreen
import com.example.medreminder.ui.settings.SettingsViewModel
import com.example.medreminder.ui.today.TodayScreen
import com.example.medreminder.ui.today.TodayViewModel
import com.example.medreminder.BuildConfig

object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val HISTORY = "history"
    const val COURSES = "courses"
    const val SETTINGS = "settings"
    const val COURSE_EDIT = "course_edit"
    const val COURSE_CREATE_TYPE = "course_create_type"
    const val COURSE_ID_ARG = "courseId"
    const val COURSE_KIND_ARG = "courseKind"

    fun courseCreateType() = COURSE_CREATE_TYPE

    fun courseEdit(courseId: Long, courseKind: CourseKind = CourseKind.MEDICATION): String =
        "$COURSE_EDIT/$courseId/${courseKind.storageKey}"

    val mainDestinations = setOf(TODAY, CALENDAR, HISTORY, COURSES, SETTINGS)
}

@Composable
fun MedReminderApp(repository: MedReminderRepository) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in Routes.mainDestinations
    val showFab = currentRoute == Routes.COURSES
    val app = LocalContext.current.applicationContext as MedReminderApplication

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.TODAY,
                        onClick = { navController.navigateTopLevel(Routes.TODAY) },
                        icon = { Icon(Icons.Default.Today, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_today)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.CALENDAR,
                        onClick = { navController.navigateTopLevel(Routes.CALENDAR) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_calendar)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { navController.navigateTopLevel(Routes.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_history)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.COURSES,
                        onClick = { navController.navigateTopLevel(Routes.COURSES) },
                        icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_medicines)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navController.navigateTopLevel(Routes.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.courseCreateType()) },
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course))
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.TODAY) {
                val vm: TodayViewModel = viewModel(factory = TodayViewModel.factory(repository, app))
                TodayScreen(
                    state = vm.uiState,
                    onAddCourse = { navController.navigate(Routes.courseCreateType()) },
                    onEditCourse = { id -> navController.navigate(Routes.courseEdit(id)) },
                    onMarkTaken = vm::markTaken,
                    onMarkSkipped = vm::markSkipped,
                    onSnooze = vm::snooze,
                )
            }
            composable(Routes.CALENDAR) {
                val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository, app))
                CalendarScreen(
                    state = vm.uiState,
                    onPreviousMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth,
                    onSelectDate = vm::selectDate,
                    onMarkTaken = vm::markTaken,
                    onMarkSkipped = vm::markSkipped,
                    onSnooze = vm::snooze,
                )
            }
            composable(Routes.HISTORY) {
                val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(repository))
                HistoryScreen(state = vm.uiState)
            }
            composable(Routes.COURSES) {
                val vm: CoursesViewModel = viewModel(factory = CoursesViewModel.factory(repository))
                CoursesScreen(
                    state = vm.uiState,
                    onEditCourse = { id, kind -> navController.navigate(Routes.courseEdit(id, kind)) },
                )
            }
            composable(Routes.SETTINGS) {
                val settingsContext = LocalContext.current
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(
                        app.userProfileRepository,
                        app.appUpdateRepository,
                        BuildConfig.VERSION_CODE,
                        BuildConfig.VERSION_NAME,
                        settingsContext,
                    ),
                )
                SettingsScreen(
                    state = vm.uiState,
                    onSelectTab = vm::selectTab,
                    onFullNameChange = vm::updateFullName,
                    onAgeChange = vm::updateAge,
                    onHeightChange = vm::updateHeight,
                    onWeightChange = vm::updateWeight,
                    onSaveProfile = vm::saveProfile,
                    onCheckUpdates = vm::checkForUpdates,
                    onDownloadUpdate = vm::downloadAndInstallUpdate,
                )
            }
            composable(Routes.COURSE_CREATE_TYPE) {
                CourseCreateTypeScreen(
                    onSelectKind = { kind ->
                        navController.navigate(Routes.courseEdit(0L, kind)) {
                            popUpTo(Routes.COURSE_CREATE_TYPE) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.COURSE_EDIT}/{${Routes.COURSE_ID_ARG}}/{${Routes.COURSE_KIND_ARG}}",
                arguments = listOf(
                    navArgument(Routes.COURSE_ID_ARG) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(Routes.COURSE_KIND_ARG) {
                        type = NavType.StringType
                        defaultValue = CourseKind.MEDICATION.storageKey
                    },
                ),
            ) { entry ->
                val courseId = entry.arguments?.getLong(Routes.COURSE_ID_ARG) ?: 0L
                val kindKey = entry.arguments?.getString(Routes.COURSE_KIND_ARG)
                val initialKind = CourseKind.fromStorage(kindKey)
                val vm: CourseEditViewModel = viewModel(
                    factory = CourseEditViewModel.factory(
                        repository,
                        app.drugReferenceRepository,
                        courseId,
                        initialKind,
                    ),
                )
                CourseEditScreen(
                    state = vm.uiState,
                    onFieldChange = vm::updateField,
                    onUnitsInPackageChange = vm::updateUnitsInPackage,
                    onAddTime = vm::addTimeSlot,
                    onRemoveTime = vm::removeTimeSlot,
                    onSoundSelected = vm::setSoundUri,
                    onLoadGrls = vm::loadGrlsReference,
                    onSelectReference = vm::selectReferenceOption,
                    onSaveReference = vm::saveSelectedReference,
                    onClearGrls = vm::clearReference,
                    onSave = {
                        vm.save {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
