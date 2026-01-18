package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long, // Начало дня
    val timestamp: Long = System.currentTimeMillis() // Точное время отметки
)