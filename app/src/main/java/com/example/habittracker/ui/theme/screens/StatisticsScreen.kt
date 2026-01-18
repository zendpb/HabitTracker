package com.example.habittracker.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.logic.LevelManager
import com.example.habittracker.ui.theme.viewmodel.StatisticsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val archivedHabits by viewModel.archivedHabits.collectAsState()
    val weakDays by viewModel.weakDays.collectAsState()

    var selectedHabitForDetail by remember { mutableStateOf<Habit?>(null) }
    val allCompletions by viewModel.allCompletions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // КАРТОЧКА ПРОГРЕССА УРОВНЯ (ОБНОВЛЕННАЯ, БЕЗ ОБВОДКИ)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Твой прогресс", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).alpha(0.7f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        stats?.let {
                            val targetXp = LevelManager.getRequiredXpForLevel(it.level)
                            val progress = it.totalXp.toFloat() / targetXp
                            val remainingXp = targetXp - it.totalXp

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${it.level}", fontSize = 56.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Column(Modifier.padding(bottom = 12.dp, start = 8.dp)) {
                                    Text("УРОВЕНЬ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("${it.totalXp} / $targetXp XP", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "До ${it.level + 1} уровня осталось $remainingXp XP",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // СЛАБЫЕ ДНИ
            if (weakDays.isNotEmpty()) {
                item {
                    Text("Общие слабые дни", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weakDays.forEach { dayOfWeek ->
                            val dayName = when(dayOfWeek) {
                                Calendar.MONDAY -> "Пн"
                                Calendar.TUESDAY -> "Вт"
                                Calendar.WEDNESDAY -> "Ср"
                                Calendar.THURSDAY -> "Чт"
                                Calendar.FRIDAY -> "Пт"
                                Calendar.SATURDAY -> "Сб"
                                Calendar.SUNDAY -> "Вс"
                                else -> ""
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(dayName) },
                                leadingIcon = { Icon(Icons.Default.TrendingDown, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            item {
                Text("Анализ по привычкам", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            items(habits.filter { !it.isArchived }, key = { it.id }) { habit ->
                HabitAnalyticsCard(habit = habit) {
                    selectedHabitForDetail = habit
                }
            }

            // АРХИВ
            if (archivedHabits.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text("Архив", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                }
                items(archivedHabits, key = { "archived_${it.id}" }) { habit ->
                    ArchivedHabitRow(habit = habit, onUnarchive = { viewModel.unarchiveHabit(habit.id) })
                }
            }
        }
    }

    if (selectedHabitForDetail != null) {
        val currentHabit = selectedHabitForDetail!!
        val completionsForHabit = allCompletions.filter { it.habitId == currentHabit.id }

        DetailAnalyticsDialog(
            habit = currentHabit,
            completions = completionsForHabit,
            onDismiss = { selectedHabitForDetail = null }
        )
    }
}

@Composable
fun HabitAnalyticsCard(habit: Habit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.icon, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Рекорд: ${habit.longestStreak} дн.", fontSize = 12.sp, color = Color.Gray)
            }
            Text("${habit.currentStreak}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(habit.color))
        }
    }
}

@Composable
fun DetailAnalyticsDialog(habit: Habit, completions: List<HabitCompletion>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${habit.icon} ${habit.name}") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Активность за 35 дней", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Spacer(Modifier.height(12.dp))

                HabitHeatMap(habitColor = Color(habit.color), completions = completions)

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Месяц назад", fontSize = 10.sp, color = Color.Gray)
                    Text("Сегодня", fontSize = 10.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(20.dp))
                AdviceBlock(habit, completions)
                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatMiniBlock("Стрик", "${habit.currentStreak}", "дн.")
                    StatMiniBlock("Рекорд", "${habit.longestStreak}", "дн.")
                    StatMiniBlock("Опыт", "${habit.xpValue}", "XP")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(habit.color))) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun HabitHeatMap(habitColor: Color, completions: List<HabitCompletion>) {
    val completedDates = completions.map {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        c.get(Calendar.DAY_OF_YEAR) to c.get(Calendar.YEAR)
    }.toSet()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (week in 0 until 5) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (dayInWeek in 0 until 7) {
                    val dayIndex = (week * 7) + dayInWeek

                    val checkCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, -(34 - dayIndex))
                    }

                    val isChecked = completedDates.contains(
                        checkCal.get(Calendar.DAY_OF_YEAR) to checkCal.get(Calendar.YEAR)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isChecked) habitColor else habitColor.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = if (isChecked) Color.Transparent else Color.LightGray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun AdviceBlock(habit: Habit, completions: List<HabitCompletion>) {
    val adviceText = remember(completions) { getHabitAdvice(completions) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(habit.color).copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, null, tint = Color(habit.color), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = adviceText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

fun getHabitAdvice(completions: List<HabitCompletion>): String {
    if (completions.isEmpty()) return "Начни отмечать выполнение, чтобы получить советы!"
    val cal = Calendar.getInstance()
    val dayCounts = mutableMapOf<Int, Int>()
    completions.forEach { completion ->
        cal.timeInMillis = completion.date
        val day = cal.get(Calendar.DAY_OF_WEEK)
        dayCounts[day] = dayCounts.getOrDefault(day, 0) + 1
    }
    val weekDays = listOf(2, 3, 4, 5, 6, 7, 1)
    val weakestDay = weekDays.minByOrNull { dayCounts.getOrDefault(it, 0) }
    return when (weakestDay) {
        Calendar.MONDAY -> "Понедельник — твой тяжелый день. Попробуй делать привычку сразу после пробуждения."
        Calendar.SATURDAY, Calendar.SUNDAY -> "На выходных дисциплина слабнет. Поставь напоминание на это время."
        else -> "Хороший темп! Главное — не пропускать более 2 дней подряд."
    }
}

@Composable
fun StatMiniBlock(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(unit, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun ArchivedHabitRow(habit: Habit, onUnarchive: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.LightGray.copy(alpha = 0.1f)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.icon, fontSize = 20.sp, modifier = Modifier.alpha(0.6f))
            Spacer(Modifier.width(12.dp))
            Text(habit.name, modifier = Modifier.weight(1f), color = Color.Gray)
            IconButton(onClick = onUnarchive) {
                Icon(Icons.Default.Unarchive, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}