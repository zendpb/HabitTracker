package com.example.habittracker.ui.theme.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.logic.BackupManager
import com.example.habittracker.logic.SmartNotificationManager

// класс-фабрика для создания вьюмоделей с нужными зависимостями
class ViewModelFactory(
    private val repository: HabitRepository,
    private val backupManager: BackupManager,
    private val notificationManager: SmartNotificationManager,
    private val application: Application
) : ViewModelProvider.Factory {

    // метод который решает какую именно вьюмодель создать
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // создаем вьюмодель для списка привычек
            modelClass.isAssignableFrom(HabitListViewModel::class.java) -> {
                HabitListViewModel(repository, notificationManager) as T
            }
            // создаем вьюмодель для экрана одной привычки
            modelClass.isAssignableFrom(HabitViewModel::class.java) -> {
                HabitViewModel(repository, notificationManager) as T
            }
            // создаем вьюмодель настроек со всеми нужными менеджерами
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    application = application,
                    repository = repository,
                    backupManager = backupManager,
                    notificationManager = notificationManager
                ) as T
            }
            // создаем вьюмодель статистики только с репозиторием
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> {
                StatisticsViewModel(repository) as T
            }
            // если пришел неизвестный класс - выдаем ошибку
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}