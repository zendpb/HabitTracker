package com.example.habittracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.data.database.AppDatabase
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.logic.BackupManager
import com.example.habittracker.logic.SmartNotificationManager
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.ui.theme.screens.*
import com.example.habittracker.ui.theme.viewmodel.*

// главная активность, точка входа в приложение
class MainActivity : ComponentActivity() {

    // штука для запроса разрешений (например, на уведомления)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // при запуске создаем канал для пушей и просим разрешение
        createNotificationChannel()
        checkNotificationPermission()

        // запускаем базу данных
        val database = AppDatabase.getDatabase(this)
        // создаем репозиторий и передаем туда все dao
        val repository = HabitRepository(
            habitDao = database.habitDao(),
            completionDao = database.completionDao(),
            statsDao = database.statsDao()
        )

        // создаем менеджеры для бэкапа и уведомлений
        val backupManager = BackupManager(this)
        val notificationManager = SmartNotificationManager(this)

        setContent {
            // создаем фабрику, которая будет прокидывать зависимости во вьюмодели
            val factory = ViewModelFactory(
                repository = repository,
                backupManager = backupManager,
                notificationManager = notificationManager,
                application = application
            )

            // берем вьюмодель настроек чтобы знать, какую тему рисовать
            val settingsVm: SettingsViewModel = viewModel(factory = factory)
            val isDarkTheme by settingsVm.isDarkTheme.collectAsState()

            // рисуем интерфейс в нужной теме
            HabitTrackerTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val habitVm: HabitViewModel = viewModel(factory = factory)

                // настраиваем навигацию между экранами
                NavHost(navController = navController, startDestination = "habit_list") {

                    // экран со списком всех привычек
                    composable("habit_list") {
                        val listVm: HabitListViewModel = viewModel(factory = factory)
                        HabitListScreen(
                            viewModel = listVm,
                            habitViewModel = habitVm,
                            onHabitClick = { id -> navController.navigate("habit_detail/$id") },
                            onAddHabit = { navController.navigate("add_habit") },
                            onSettings = { navController.navigate("settings") },
                            onArchiveClick = { navController.navigate("statistics") }
                        )
                    }

                    // экран деталей конкретной привычки
                    composable(
                        "habit_detail/{habitId}",
                        arguments = listOf(navArgument("habitId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val habitId = backStackEntry.arguments?.getString("habitId") ?: ""
                        HabitDetailScreen(
                            habitId = habitId,
                            viewModel = habitVm,
                            settingsViewModel = settingsVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // экран создания новой привычки
                    composable("add_habit") {
                        AddHabitScreen(
                            habitViewModel = habitVm,
                            onSave = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    // экран настроек приложения
                    composable("settings") {
                        SettingsScreen(
                            settingsVm = settingsVm,
                            onOpenAnalytics = { navController.navigate("statistics") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // экран статистики и архива
                    composable("statistics") {
                        val statsVm: StatisticsViewModel = viewModel(factory = factory)
                        StatisticsScreen(
                            viewModel = statsVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // проверяем есть ли права на показ пушей (для 13 андроида и выше)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // регистрируем канал уведомлений в системе
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "habit_reminders"
            val name = "Напоминания о привычках"
            val descriptionText = "Уведомления, которые помогают не забывать о ваших целях"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}