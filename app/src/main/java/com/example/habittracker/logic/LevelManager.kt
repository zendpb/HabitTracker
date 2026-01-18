package com.example.habittracker.logic

import com.example.habittracker.data.entity.UserStats

// класс для управления уровнями и опытом
class LevelManager {
    companion object {
        // считаем сколько нужно опыта для перехода на следующий уровень
        // формула  100 + текущий уровень * 50
        fun getRequiredXpForLevel(level: Int): Int {
            return 100 + (level * 50)
        }

        //fun processNewXp(currentStats: UserStats, xpToAdd: Int): UserStats {
        //    var newTotalXp = currentStats.totalXp + xpToAdd
        //    var newLevel = currentStats.level
        //    while (newTotalXp >= getRequiredXpForLevel(newLevel)) {
        //        newTotalXp -= getRequiredXpForLevel(newLevel)
        //        newLevel++
        //    }
        //    return currentStats.copy(level = newLevel, totalXp = newTotalXp)
        //}


        //fun removeXp(currentStats: UserStats, xpToRemove: Int): UserStats {
        //    var newTotalXp = currentStats.totalXp - xpToRemove
        //    var newLevel = currentStats.level

        //    while (newTotalXp < 0 && newLevel > 1) {
        //        newLevel--
        //        newTotalXp += getRequiredXpForLevel(newLevel)
        //    }

        //    return currentStats.copy(
        //        level = newLevel,
        //        totalXp = newTotalXp.coerceAtLeast(0)
        //    )
        //}
    }
}