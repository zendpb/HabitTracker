package com.example.habittracker.data.dao

import androidx.room.*
import com.example.habittracker.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getHabitById(habitId: String): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitByIdSync(habitId: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
}

@Dao
interface CompletionDao {


    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsByDateFlow(date: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun getCompletion(habitId: String, date: Long): HabitCompletion?

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletionsSync(): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getAllCompletedDates(habitId: String): Flow<List<HabitCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: HabitCompletion)

    @Delete
    suspend fun delete(completion: HabitCompletion)
}



@Dao
interface StatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 0")
    fun getStats(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: UserStats)
}