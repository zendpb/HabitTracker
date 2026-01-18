package com.example.habittracker.ui.theme.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.ui.theme.viewmodel.HabitViewModel
import com.example.habittracker.ui.theme.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: HabitViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val habit by viewModel.selectedHabit.collectAsState()
    val completions by viewModel.completedDates.collectAsState()
    val isAdminMode by settingsViewModel.isAdminMode.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(habitId) { viewModel.loadHabit(habitId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit?.name ?: "Детали") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (habit != null) {
            val h = habit!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val isDoneToday = completions.any { it.date == today }
            val isGoalReached = h.targetDays > 0 && h.currentStreak >= h.targetDays

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                if (isGoalReached) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF4CAF50).copy(0.1f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏆", fontSize = 32.sp)
                                Text("Цель в ${h.targetDays} дн. достигнута!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(h.color).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(viewModel.getEvolutionIcon(h.currentStreak), fontSize = 72.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(h.name, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Text(h.icon, fontSize = 20.sp)
                        }
                    }
                }

                if (h.description.isNotBlank()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Text("Подробнее", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(text = h.description, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.toggleHabitFromDetail(h.id) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDoneToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (isDoneToday) Icons.Default.Close else Icons.Default.Check, null)
                        Spacer(Modifier.width(12.dp))
                        Text(if (isDoneToday) "Снять отметку" else "Отметить выполнение", fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("Последние 7 дней", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val days = (0..6).reversed().map { i ->
                                Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -i)
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }
                            }

                            days.forEach { cal ->
                                val time = cal.timeInMillis
                                val isDone = completions.any { it.date == time }
                                val dayName = SimpleDateFormat("EE", Locale("ru")).format(cal.time)

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isDone) Color(h.color) else Color.LightGray.copy(0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(dayName, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailStatCard("Стрик", "${h.currentStreak}", Icons.Default.Whatshot, Color(0xFFFF5722), Modifier.weight(1f))
                            DetailStatCard("Рекорд", "${h.longestStreak}", Icons.Default.EmojiEvents, Color(0xFFFFC107), Modifier.weight(1f))
                        }
                        val targetText = if(h.targetDays == 0) "∞" else "${h.targetDays}"
                        DetailStatCard("Цель", targetText, Icons.Default.Flag, Color(0xFF2196F3), Modifier.fillMaxWidth())
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.archiveHabit(h.id, !h.isArchived) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if(h.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if(h.isArchived) "Вернуть из архива" else "Архивировать привычку")
                    }
                }

                item { Text("История", fontWeight = FontWeight.Bold, fontSize = 18.sp) }

                items(completions.sortedByDescending { it.date }.take(10)) { completion ->
                    val dateStr = SimpleDateFormat("dd MMMM, EEEE", Locale("ru")).format(Date(completion.date))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(dateStr, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить привычку?") },
            text = { Text("Это действие нельзя будет отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    habit?.let { viewModel.deleteHabit(it.id) }
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (showEditDialog && habit != null) {
        EditHabitDialog(
            habit = habit!!,
            isAdmin = isAdminMode,
            onDismiss = { showEditDialog = false },
            onSave = { n, d, i, t, streak, record, time, adaptive ->
                viewModel.updateHabitFull(
                    id = habit!!.id,
                    name = n,
                    description = d,
                    icon = i,
                    target = t.toIntOrNull() ?: 0,
                    streak = streak.toIntOrNull() ?: habit!!.currentStreak,
                    record = record.toIntOrNull() ?: habit!!.longestStreak,
                    reminderTime = time,
                    isAdaptive = adaptive
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
fun DetailStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, color.copy(0.2f))) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
    }
}

@Composable
fun EditHabitDialog(
    habit: Habit,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var n by remember { mutableStateOf(habit.name) }
    var d by remember { mutableStateOf(habit.description) }
    var i by remember { mutableStateOf(habit.icon) }
    var t by remember { mutableStateOf(habit.targetDays.toString()) }
    var streak by remember { mutableStateOf(habit.currentStreak.toString()) }
    var record by remember { mutableStateOf(habit.longestStreak.toString()) }

    var reminderTime by remember { mutableStateOf(habit.reminderTime) }
    var isAdaptive by remember { mutableStateOf(habit.isAdaptiveReminder) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            reminderTime = String.format("%02d:%02d", hour, minute)
            isAdaptive = false
        },
        12, 0, true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = n, onValueChange = { n = it.replace("\n", "") }, label = { Text("Название") })

                Text("Уведомления", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAdaptive, onCheckedChange = {
                        isAdaptive = it
                        if(it) reminderTime = null
                    })
                    Text("Адаптивное время", fontSize = 14.sp)
                }

                if(!isAdaptive) {
                    OutlinedButton(onClick = { timePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                        Text(reminderTime ?: "Выбрать время")
                    }
                }

                OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("Описание") })
                OutlinedTextField(value = i, onValueChange = { i = it }, label = { Text("Emoji") })
                OutlinedTextField(
                    value = t,
                    onValueChange = { if(it.all { c -> c.isDigit() }) t = it },
                    label = { Text("Цель (дней)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (isAdmin) {
                    Divider(Modifier.padding(vertical = 8.dp))
                    Text("разработчик", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = streak, onValueChange = { streak = it }, label = { Text("Стрик") })
                    OutlinedTextField(value = record, onValueChange = { record = it }, label = { Text("Рекорд") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(n, d, i, t, streak, record, reminderTime, isAdaptive) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}