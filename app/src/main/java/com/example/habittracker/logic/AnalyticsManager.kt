package com.example.habittracker.logic

import com.example.habittracker.data.entity.HabitCompletion
import java.util.*

// считаем статистику по дням
class AnalyticsManager {
    // ищем дни где меньше всего отметок
    fun identifyWeakDays(completions: List<HabitCompletion>): List<Int> {
        // список для подсчета: день недели -> сколько раз сделали
        val dayCounts = mutableMapOf<Int, Int>()
        val cal = Calendar.getInstance()

        // считаем сколько раз попали на каждый день недели
        completions.forEach {
            cal.timeInMillis = it.date
            val day = cal.get(Calendar.DAY_OF_WEEK)
            dayCounts[day] = dayCounts.getOrDefault(day, 0) + 1
        }

        // берем 2 самых ленивых дня (где меньше всего записей)
        return dayCounts.entries.sortedBy { it.value }.take(2).map { it.key }
    }
}