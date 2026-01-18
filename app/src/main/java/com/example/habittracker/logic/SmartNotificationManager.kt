package com.example.habittracker.logic

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import java.util.*


class SmartNotificationManager(private val context: Context) {

    // подключаем будильник и сервис уведомлений системы
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "habit_reminders_channel"

    // при создании сразу настраиваем канал для уведомлений
    init {
        createNotificationChannel()
    }

    // создаем канал, чтобы андроид понимал куда слать пуши
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Habit Reminders"
            val descriptionText = "Notifications for your daily habits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // шлем уведомление прямо сейчас (для тестов или инфы о бэкапе)
    fun sendImmediateNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // выводим пуш с уникальным id через время
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // считаем лучшее время для напоминания на основе прошлых отметок
    fun calculateAdaptiveTime(completions: List<HabitCompletion>): Pair<Int, Int> {
        // если еще не отмечали то ставим на 9 утра по дефолту
        if (completions.isEmpty()) return Pair(9, 0)

        // считаем в какие часы чаще всего жмакали привычку
        val hourCounts = completions.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.HOUR_OF_DAY)
        }.groupingBy { it }.eachCount()

        // берем час который встречается чаще всего
        val bestHour = hourCounts.maxByOrNull { it.value }?.key ?: 9
        return Pair(bestHour, 0)
    }

    // ставим будильник на уведомление
    fun scheduleNotification(habit: Habit, hour: Int, minute: Int) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_NAME", habit.name)
            putExtra("HABIT_ID", habit.id)
        }

        // готовим интент для отправки в систему
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ставим время срабатывания
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // если время уже прошло сегодня, ставим на завтра
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // проверяем права на точные будильники для новых версий андроид
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    // ставим точный будильник который сработает даже в спящем режиме
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // если прав нет, ставим неточный
                    scheduleInexactNotification(habit, hour, minute)
                }
            } else {
                scheduleInexactNotification(habit, hour, minute)
            }
        } else {
            // для старых систем просто ставим точный будильник
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    // ставим неточное уведомление если нет прав на точное
    private fun scheduleInexactNotification(habit: Habit, hour: Int, minute: Int) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_NAME", habit.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        // система сама решит когда запустить в районе этого времени
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    // отменяем уведомление для привычки
    fun cancelNotification(habitId: String) {
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // удаляем из очереди будильников
        alarmManager.cancel(pendingIntent)
    }
}