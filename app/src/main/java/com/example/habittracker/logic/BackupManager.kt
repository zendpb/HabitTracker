package com.example.habittracker.logic

import android.content.Context
import android.net.Uri
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// модель для хранения всех данных в одном файле
data class BackupData(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>
)

// класс для сохранения и загрузки бэкапа
class BackupManager(private val context: Context) {
    private val gson = Gson()

    // превращаем данные в текст и сохраняем в файл по ссылке uri
    fun exportData(uri: Uri, habits: List<Habit>, completions: List<HabitCompletion>): Boolean {
        return try {
            // собираем привычки и отметки в одну кучу
            val data = BackupData(habits, completions)
            // переводим все это в json строку
            val jsonString = gson.toJson(data)
            // открываем файл и записываем туда текст
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer -> writer.write(jsonString) }
            }
            true
        } catch (e: Exception) { false }
    }

    // читаем файл по ссылке uri и превращаем текст обратно в данные
    fun importData(uri: Uri): BackupData? {
        return try {
            // открываем файл для чтения
            context.contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins)).use { reader ->
                    // читаем весь текст целиком
                    val json = reader.readText()
                    // указываем формат данных для парсинга
                    val type = object : TypeToken<BackupData>() {}.type
                    // превращаем json строку в объекты
                    gson.fromJson<BackupData>(json, type)
                }
            }
        } catch (e: Exception) { null }
    }
}