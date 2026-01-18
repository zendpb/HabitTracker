package com.example.habittracker.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.data.entity.UserStats
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.logic.AnalyticsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// вьюмодель для экрана статистики и архива
class StatisticsViewModel(private val repository: HabitRepository) : ViewModel() {
    // подключаем логику анализа данных
    private val analyticsManager = AnalyticsManager()

    // получаем данные об уровне и опыте из базы
    val stats: StateFlow<UserStats?> = repository.getUserStats()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // берем вообще все привычки какие есть
    val allHabits: StateFlow<List<Habit>> = repository.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // фильтруем список, оставляя только те, что в архиве
    val archivedHabits: StateFlow<List<Habit>> = repository.getAllHabits()
        .map { list -> list.filter { it.isArchived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // берем вообще все отметки о выполнении для календаря активности
    val allCompletions: StateFlow<List<HabitCompletion>> = flow {
        emit(repository.getAllCompletionsSync())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ищем дни недели, когда юзер чаще всего пропускает привычки
    val weakDays: StateFlow<List<Int>> = flow {
        val completions = repository.getAllCompletionsSync()
        emit(analyticsManager.identifyWeakDays(completions))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // функция чтобы достать привычку из архива обратно в список
    fun unarchiveHabit(habitId: String) {
        viewModelScope.launch {
            // меняем статус архивации на "выкл"
            repository.archiveHabit(habitId, false)
        }
    }
}