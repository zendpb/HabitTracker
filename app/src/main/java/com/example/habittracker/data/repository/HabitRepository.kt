package com.example.habittracker.data.repository

import com.example.habittracker.data.dao.*
import com.example.habittracker.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*

// главный класс для работы с данными
class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao,
    private val statsDao: StatsDao
) {

    // берем время начала сегодня (00:00) в миллисекундах
    fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // поток для обновления галочек на главном экране в реальном времени
    fun getTodayCompletionsFlow(): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsByDateFlow(getStartOfDay())
    }

    // достаем все привычки списком
    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()

    // ищем одну привычку по id
    fun getHabitById(id: String): Flow<Habit?> = habitDao.getHabitById(id)

    // берем одну привычку по id без подписки на изменения
    suspend fun getHabitByIdSync(id: String): Habit? = habitDao.getHabitByIdSync(id)

    // получаем уровень и опыт игрока
    fun getUserStats(): Flow<UserStats?> = statsDao.getStats()

    // достаем все даты, когда привычка была выполнена
    fun getAllCompletedDates(habitId: String): Flow<List<HabitCompletion>> = completionDao.getAllCompletedDates(habitId)

    // создание новой привычки
    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)

    // сохранение изменений в привычке
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    // удаление привычки из базы
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    // проверяем, была ли отметка в конкретный день
    suspend fun getCompletionByDateSync(habitId: String, date: Long): HabitCompletion? {
        return completionDao.getCompletion(habitId, date)
    }

    // закидываем сразу кучу данных (нужно для импорта бэкапа)
    suspend fun restoreAllData(habits: List<Habit>, completions: List<HabitCompletion>) {
        habits.forEach { habitDao.insertHabit(it) }
        completions.forEach { completionDao.insert(it) }
    }

    // обновляем уровень и опыт вручную
    suspend fun updateGlobalStats(level: Int, xp: Int) {
        val current = statsDao.getStats().first() ?: UserStats()
        statsDao.updateStats(current.copy(level = level, totalXp = xp))
    }

    // когда жмем "выполнить": сохраняем отметку и растим стрик
    suspend fun insertCompletion(completion: HabitCompletion) {
        completionDao.insert(completion)
        val habit = habitDao.getHabitByIdSync(completion.habitId) ?: return

        // прибавляем 1 к серии и обновляем рекорд, если надо
        habitDao.updateHabit(habit.copy(
            currentStreak = habit.currentStreak + 1,
            longestStreak = if (habit.currentStreak + 1 > habit.longestStreak) habit.currentStreak + 1 else habit.longestStreak,
            lastCompletedDate = completion.date
        ))
    }

    // когда отменяем выполнение: удаляем отметку и откатываем стрик
    suspend fun deleteCompletion(completion: HabitCompletion) {
        completionDao.delete(completion)
        val habit = habitDao.getHabitByIdSync(completion.habitId) ?: return

        // ищем предыдущую дату выполнения, чтобы вернуть ее в инфу о привычке
        val allCompletions = completionDao.getAllCompletedDates(habit.id).first()
            .filter { it.id != completion.id }
            .sortedByDescending { it.date }

        val lastDate = if (allCompletions.isNotEmpty()) allCompletions.first().date else null

        // убираем 1 из серии и ставим старую дату выполнения
        habitDao.updateHabit(habit.copy(
            currentStreak = (habit.currentStreak - 1).coerceAtLeast(0),
            lastCompletedDate = lastDate
        ))
    }

    // прячем привычку в архив или достаем обратно
    suspend fun archiveHabit(habitId: String, archived: Boolean) {
        val habit = habitDao.getHabitByIdSync(habitId) ?: return
        habitDao.updateHabit(habit.copy(isArchived = archived))
    }

    // просто обновляем статистику юзера
    suspend fun updateUserStats(stats: UserStats) {
        statsDao.updateStats(stats)
    }

    // меняем число серии дней вручную
    //suspend fun setStreakValue1(habitId: String, value: Int) {
    //    val habit = habitDao.getHabitByIdSync(habitId) ?: return
    //    habitDao.updateHabit(habit.copy(currentStreak = value))
    //}

    // достаем вообще все отметки из базы для файла бэкапа
    suspend fun getAllCompletionsSync(): List<HabitCompletion> {
        return completionDao.getAllCompletionsSync()
    }
}