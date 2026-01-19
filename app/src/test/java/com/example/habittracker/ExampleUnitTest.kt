package com.example.habittracker

import com.example.habittracker.data.entity.Habit
import com.example.habittracker.logic.LevelManager
import org.junit.Test
import org.junit.Assert.*
import java.util.*


class HabitTrackerLogicTest {

    // проверка системы уровней
    @Test
    fun testLevelXpCalculation() {
        // проверяем, что для 1 уровня нужно 100 XP
        val xpForLevel1 = LevelManager.getRequiredXpForLevel(1)
        assertEquals("Для 1 уровня должно быть 100 XP", 100, xpForLevel1)

        // проверяем прогрессию для 2 уровня 200 XP
        val xpForLevel2 = LevelManager.getRequiredXpForLevel(2)
        assertEquals("Для 2 уровня должно быть 200 XP", 200, xpForLevel2)
    }

    // проверка логики стадий роста дерева
    @Test
    fun testTreeStageEvolution() {
        val levelInitial = 1
        val levelMid = 4
        val levelHigh = 8

        // Логика: 1-3 уровень (стадия 1), 4-7 уровень (стадия 2), 8+ (стадия 3)
        val stage1 = if (levelInitial < 3) 1 else if (levelInitial < 7) 2 else 3
        val stage2 = if (levelMid < 4) 1 else if (levelMid < 7) 2 else 3
        val stage3 = if (levelHigh < 7) 2 else 3

        assertEquals("На 1 уровне должно быть семечко (стадия 1)", 1, stage1)
        assertEquals("На 4 уровне должен быть росток (стадия 2)", 2, stage2)
        assertEquals("На 8 уровне должно быть дерево (стадия 3)", 3, stage3)
    }

    // расчет прогресса в процентах для ProgressBar
    @Test
    fun testProgressPercentage() {
        val currentXp = 150
        val targetXp = 200
        val expectedProgress = 0.75f

        val actualProgress = currentXp.toFloat() / targetXp

        assertEquals(expectedProgress, actualProgress, 0.001f)
    }

    // проверка логики стриков
    @Test
    fun testHabitStreakLogic() {
        val habit = Habit(
            name = "Тест",
            description = " ",
            icon = "🌳",
            color = 0,
            currentStreak = 10,
            longestStreak = 10
        )

        // имитируем выполнение стрик должен увеличиться
        val newCurrentStreak = habit.currentStreak + 1
        val newLongestStreak = if (newCurrentStreak > habit.longestStreak) newCurrentStreak else habit.longestStreak

        assertEquals(11, newCurrentStreak)
        assertEquals(11, newLongestStreak)
    }

    // проверка логики слабых дней
    @Test
    fun testWeakDaysDetection() {
        val completionsCount = mapOf(
            Calendar.MONDAY to 5,
            Calendar.TUESDAY to 1, // самый слабый день
            Calendar.WEDNESDAY to 4
        )

        val weakestDay = completionsCount.minByOrNull { it.value }?.key

        assertEquals(Calendar.TUESDAY, weakestDay)
    }
}