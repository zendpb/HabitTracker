package com.example.habittracker.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.logic.SmartNotificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// вьюмодель для экрана одной привычки
class HabitViewModel(
    private val repository: HabitRepository,
    private val notificationManager: SmartNotificationManager
) : ViewModel() {

    // тут лежит выбранная привычка
    private val _selectedHabit = MutableStateFlow<Habit?>(null)
    val selectedHabit: StateFlow<Habit?> = _selectedHabit.asStateFlow()

    // тут список всех дней когда привычку сделали
    private val _completedDates = MutableStateFlow<List<HabitCompletion>>(emptyList())
    val completedDates: StateFlow<List<HabitCompletion>> = _completedDates.asStateFlow()

    // загружаем данные привычки по id
    fun loadHabit(habitId: String) {
        viewModelScope.launch {
            // следим за изменениями в базе
            repository.getHabitById(habitId).collect { habit ->
                _selectedHabit.value = habit
            }
        }
        viewModelScope.launch {
            // следим за новыми отметками в календаре
            repository.getAllCompletedDates(habitId).collect { dates ->
                _completedDates.value = dates
            }
        }
    }

    // кнопка выполнить/отменить на экране деталей
    fun toggleHabitFromDetail(habitId: String) {
        viewModelScope.launch {
            val habit = repository.getHabitByIdSync(habitId) ?: return@launch
            // берем время начала дня
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val existing = repository.getCompletionByDateSync(habitId, today)
            if (existing != null) {
                // если уже была отметка - удаляем и снова ставим уведомление
                repository.deleteCompletion(existing)
                val completions = repository.getAllCompletedDates(habitId).first()
                val (h, m) = notificationManager.calculateAdaptiveTime(completions)
                notificationManager.scheduleNotification(habit, h, m)
            } else {
                // если отметки нет - добавляем и выключаем пуш на сегодня
                repository.insertCompletion(HabitCompletion(habitId = habitId, date = today))
                notificationManager.cancelNotification(habitId)
            }
        }
    }

    // сохраняем все правки в привычке
    fun updateHabitFull(id: String, name: String, description: String, icon: String, target: Int, streak: Int, record: Int, reminderTime: String?, isAdaptive: Boolean) {
        viewModelScope.launch {
            val current = repository.getHabitByIdSync(id) ?: return@launch
            val updated = current.copy(
                name = name,
                description = description,
                icon = icon,
                targetDays = target,
                currentStreak = streak,
                longestStreak = record,
                reminderTime = reminderTime,
                isAdaptiveReminder = isAdaptive
            )
            repository.updateHabit(updated)
            _selectedHabit.value = updated

            // после правок меняем время пуша
            if (isAdaptive) {
                // если умный режим - считаем время сами
                val completions = repository.getAllCompletedDates(id).first()
                val (h, m) = notificationManager.calculateAdaptiveTime(completions)
                notificationManager.scheduleNotification(updated, h, m)
            } else if (reminderTime != null) {
                // если ручной режим - берем время из строки
                val parts = reminderTime.split(":")
                notificationManager.scheduleNotification(updated, parts[0].toInt(), parts[1].toInt())
            } else {
                // если время не указано - удаляем пуш
                notificationManager.cancelNotification(id)
            }
        }
    }

    // удаляем привычку совсем
    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.getHabitByIdSync(habitId)?.let { habit ->
                // сначала выключаем уведомление
                notificationManager.cancelNotification(habit.id)
                repository.deleteHabit(habit)
            }
        }
    }

    // убираем в архив или достаем
    fun archiveHabit(habitId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveHabit(habitId, archived)
            _selectedHabit.value = _selectedHabit.value?.copy(isArchived = archived)
            if (archived) {
                // если в архиве - пуши не нужны
                notificationManager.cancelNotification(habitId)
            } else {
                // если достали - ставим стандартный пуш на 9 утра
                val habit = repository.getHabitByIdSync(habitId) ?: return@launch
                val (h, m) = notificationManager.calculateAdaptiveTime(emptyList())
                notificationManager.scheduleNotification(habit, h, m)
            }
        }
    }

    // иконка роста в зависимости от серии дней
    fun getEvolutionIcon(streak: Int): String {
        return when {
            streak >= 30 -> "🎋"
            streak >= 14 -> "🌲"
            streak >= 7 -> "🌿"
            streak >= 3 -> "🌱"
            else -> "🌰"
        }
    }

    // создаем новую привычку
    fun addHabit(name: String, description: String, icon: String, color: Int, xp: Int, targetDays: Int, reminderTime: String?, isAdaptive: Boolean) {
        viewModelScope.launch {
            val newHabit = Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                icon = icon,
                color = color,
                xpValue = xp,
                targetDays = targetDays,
                reminderTime = reminderTime,
                isAdaptiveReminder = isAdaptive
            )
            repository.insertHabit(newHabit)
            // настраиваем пуш сразу после создания
            if (isAdaptive) {
                notificationManager.scheduleNotification(newHabit, 9, 0)
            } else if (reminderTime != null) {
                val parts = reminderTime.split(":")
                notificationManager.scheduleNotification(newHabit, parts[0].toInt(), parts[1].toInt())
            }
        }
    }
}