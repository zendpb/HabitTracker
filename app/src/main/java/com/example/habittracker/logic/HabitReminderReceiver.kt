package com.example.habittracker.logic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build

// класс который ловит сигналы от системы и показывает уведомления
class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // достаем название привычки из данных интента
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Привычка"
        val channelId = "habit_reminders"

        // получаем доступ к управлению уведомлениями в системе
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // создаем канал уведомлений если телефон на новой версии android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Напоминания", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // собираем само уведомление: заголовок, текст и иконка
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Пора действовать!")
            .setContentText("Не забудь: $habitName")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true) // убираем уведомление после нажатия
            .build()

        // выводим уведомление на экран (id берем из названия чтобы не дублировать)
        notificationManager.notify(habitName.hashCode(), notification)
    }
}