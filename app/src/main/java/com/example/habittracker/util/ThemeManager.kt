package com.example.habittracker.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var isDarkTheme = mutableStateOf(prefs.getBoolean("is_dark", false))

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
        prefs.edit { putBoolean("is_dark", isDarkTheme.value) }
    }
}