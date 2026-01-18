package com.example.habittracker.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.UserStats
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.logic.SmartNotificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// вьюмодель для главного экрана со списком
class HabitListViewModel(
    private val repository: HabitRepository,
    private val notificationManager: SmartNotificationManager
) : ViewModel() {

    // получаем список всех привычек, кроме архивных
    val habits: StateFlow<List<Habit>> = repository.getAllHabits()
        .map { list -> list.filter { !it.isArchived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // получаем данные об уровне и опыте юзера
    val userStats: StateFlow<UserStats> = repository.getUserStats()
        .map { it ?: UserStats() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserStats())

    // получаем список id привычек, которые уже сделали сегодня
    val todayCompletions: StateFlow<Set<String>> = repository.getTodayCompletionsFlow()
        .map { completions -> completions.map { it.habitId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // при запуске проверяем не пропущены ли дни
    init {
        updateTodayStatus()
    }

    // сбрасываем серию (стрик) в ноль, если вчера привычку не сделали
    fun updateTodayStatus() {
        viewModelScope.launch {
            val allHabits = repository.getAllHabits().first()
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            allHabits.forEach { habit ->
                val lastComp = habit.lastCompletedDate ?: 0L
                // если последняя отметка была раньше вчерашнего дня — сброс
                if (lastComp != 0L && lastComp < yesterday && habit.currentStreak > 0) {
                    repository.updateHabit(habit.copy(currentStreak = 0))
                }
            }
        }
    }

    // переключаем состояние привычки: выполнено или нет
    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val startOfDay = repository.getStartOfDay()
            val isCompleted = todayCompletions.value.contains(habit.id)

            if (isCompleted) {
                // если была сделана — удаляем отметку и отнимаем опыт
                val existing = repository.getCompletionByDateSync(habit.id, startOfDay)
                if (existing != null) {
                    repository.deleteCompletion(existing)
                    addExperience(-habit.xpValue)
                    rescheduleNotification(habit)
                }
            } else {
                // если не сделана — добавляем отметку и даем опыт
                repository.insertCompletion(HabitCompletion(habitId = habit.id, date = startOfDay))
                addExperience(habit.xpValue)
                // отменяем уведомление на сегодня и ставим на потом
                notificationManager.cancelNotification(habit.id)
                rescheduleNotification(habit)
            }
        }
    }

    // пересчитываем время уведомления под привычки юзера
    private suspend fun rescheduleNotification(habit: Habit) {
        val completions = repository.getAllCompletedDates(habit.id).first()
        val (hour, minute) = notificationManager.calculateAdaptiveTime(completions)
        notificationManager.scheduleNotification(habit, hour, minute)
    }

    // логика начисления опыта и повышения уровня
    private fun addExperience(amount: Int) {
        viewModelScope.launch {
            val currentStats = userStats.value
            var newXp = currentStats.totalXp + amount
            var newLevel = currentStats.level
            var nextLevelThreshold = 100 + (newLevel * 50)

            // если опыта много — повышаем уровень
            while (newXp >= nextLevelThreshold) {
                newXp -= nextLevelThreshold
                newLevel++
                nextLevelThreshold = 100 + (newLevel * 50)
            }
            // если опыт ушел в минус (при отмене) — понижаем уровень
            while (newXp < 0 && newLevel > 1) {
                newLevel--
                newXp += 100 + (newLevel * 50)
            }
            if (newXp < 0) newXp = 0

            // сохраняем обновленную статистику
            repository.updateUserStats(currentStats.copy(level = newLevel, totalXp = newXp))
        }
    }
}