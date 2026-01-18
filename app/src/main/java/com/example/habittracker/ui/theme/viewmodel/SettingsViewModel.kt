package com.example.habittracker.ui.theme.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.entity.UserStats
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.logic.BackupManager
import com.example.habittracker.logic.SmartNotificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// вьюмодель для настроек приложения
class SettingsViewModel(
    application: Application,
    private val repository: HabitRepository,
    private val backupManager: BackupManager,
    private val notificationManager: SmartNotificationManager
) : AndroidViewModel(application) {

    // доступ к памяти телефона для сохранения настроек
    private val sharedPrefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    // следим за темной темой
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // следим включен ли админ-режим (для редактирования опыта)
    private val _isAdminMode = MutableStateFlow(sharedPrefs.getBoolean("admin_mode", false))
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // следим включены ли пуши вообще
    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // берем данные об уровне и опыте из базы
    val userStats: StateFlow<UserStats> = repository.getUserStats()
        .map { it ?: UserStats() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserStats())

    // переключаем тему и сохраняем выбор в память
    fun toggleDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        sharedPrefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    // переключаем режим админа и сохраняем в память
    fun toggleAdminMode(enabled: Boolean) {
        _isAdminMode.value = enabled
        sharedPrefs.edit().putBoolean("admin_mode", enabled).apply()
    }

    // включаем или выключаем уведомления в памяти
    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("notifications", enabled).apply()
    }

    // создаем резервную копию всех данных
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            // берем все привычки и все отметки
            val habits = repository.getAllHabits().first()
            val completions = repository.getAllCompletionsSync()
            // сохраняем в файл по ссылке
            val success = backupManager.exportData(uri, habits, completions)
            if (success) {
                // если все ок - шлем пуш об успехе
                notificationManager.sendImmediateNotification("Успех", "Данные успешно экспортированы")
            }
        }
    }

    // восстанавливаем данные из файла
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            // читаем данные из файла
            val data = backupManager.importData(uri)
            if (data != null) {
                // если файл прочитан - заливаем все в базу
                repository.restoreAllData(data.habits, data.completions)
                notificationManager.sendImmediateNotification("Успех", "Данные восстановлены. Перезапустите экран.")
            } else {
                // если файл битый или пустой - шлем пуш с ошибкой
                notificationManager.sendImmediateNotification("Ошибка", "Не удалось прочитать файл резервной копии")
            }
        }
    }

    // кнопка для проверки работы уведомлений
    fun sendTestNotification() {
        notificationManager.sendImmediateNotification(
            "Тест",
            "Уведомления работают корректно"
        )
    }

    // меняем уровень вручную (только для админа)
    fun updateLevel(newLevel: Int) {
        viewModelScope.launch {
            // сохраняем новый уровень, оставляя старый опыт
            repository.updateGlobalStats(level = newLevel, xp = userStats.value.totalXp)
        }
    }

    // меняем опыт вручную (только для админа)
    fun updateXP(newXp: Int) {
        viewModelScope.launch {
            val current = userStats.value
            // сохраняем новый опыт, оставляя старый уровень
            repository.updateGlobalStats(level = current.level, xp = newXp)
        }
    }
}