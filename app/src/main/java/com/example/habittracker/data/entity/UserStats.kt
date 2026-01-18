package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val treeStage: Int = 1, // 1  семечко 5  максимум
    val totalCoins: Int = 0,
    val totalCompletions: Int = 0
)