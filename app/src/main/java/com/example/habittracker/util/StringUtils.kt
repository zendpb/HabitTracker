package com.example.habittracker.util

object StringUtils {
    /**
     * Склонение существительных: 1 шаг, 2 шага, 5 шагов
     */
    fun getRussianPlural(number: Int, one: String, two: String, five: String): String {
        val n = Math.abs(number) % 100
        val n1 = n % 10
        if (n > 10 && n < 20) return five
        if (n1 > 1 && n1 < 5) return two
        if (n1 == 1) return one
        return five
    }

    fun formatHabitsLeft(count: Int): String {
        // Используем "привычку" для 1, "привычки" для 2-4 и "привычек" для 5+
        val word = getRussianPlural(count, "привычку", "привычки", "привычек")
        return "Осталось выполнить: $count $word"
    }
}