package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val color: Int,
    val icon: String = "🌱",
    val xpValue: Int = 15,
    val targetDays: Int = 0, // 0  бесконечно  >0  цель в днях
    val reminderTime: String? = null,
    val isAdaptiveReminder: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: Long? = null,
    val isArchived: Boolean = false
)