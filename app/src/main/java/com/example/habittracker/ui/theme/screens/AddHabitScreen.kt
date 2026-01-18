package com.example.habittracker.ui.theme.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.ui.theme.viewmodel.HabitViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitScreen(
    habitViewModel: HabitViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🌱") }
    var isInfinite by remember { mutableStateOf(true) }
    var targetDaysInput by remember { mutableStateOf("21") }

    // Новые состояния для времени
    var reminderTime by remember { mutableStateOf<String?>(null) }
    var isAdaptive by remember { mutableStateOf(true) }

    val colors = listOf(
        0xFF42A5F5, 0xFF66BB6A, 0xFFFFA726, 0xFFAB47BC, 0xFFEF5350,
        0xFF26C6DA, 0xFFEC407A, 0xFF78909C, 0xFF8D6E63, 0xFFFFEE58
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            reminderTime = String.format("%02d:%02d", hour, minute)
            isAdaptive = false
        },
        12, 0, true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая привычка", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(selectedColor).copy(0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(selectedIcon, fontSize = 30.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace("\n", "") },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = desc,
                onValueChange = {
                    val lines = it.count { char -> char == '\n' }
                    if (lines < 3) desc = it
                },
                label = { Text("Описание (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text("Напоминание", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isAdaptive, onCheckedChange = {
                    isAdaptive = it
                    if (it) reminderTime = null
                })
                Text("Умное напоминание (адаптивное)", fontSize = 14.sp)
            }

            if (!isAdaptive) {
                OutlinedButton(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, null)
                    Spacer(Modifier.width(8.dp))
                    Text(reminderTime ?: "Выбрать время")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Цель привычки", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isInfinite,
                    onClick = { isInfinite = true },
                    label = { Text("Бесконечно") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = !isInfinite,
                    onClick = { isInfinite = false },
                    label = { Text("На срок") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (!isInfinite) {
                OutlinedTextField(
                    value = targetDaysInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetDaysInput = it },
                    label = { Text("Количество дней") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Text("дн. ", color = Color.Gray) }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Иконка", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val icons = listOf("🌱", "💧", "🏃", "📚", "🧘", "🍎", "😴", "🎸", "💪", "🎨")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(icons) { icon ->
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedIcon == icon) Color(selectedColor).copy(0.2f) else Color.Transparent)
                            .clickable { selectedIcon = icon },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 24.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Цвет", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .clickable { selectedColor = colorHex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == colorHex) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalTarget = if (isInfinite) 0 else (targetDaysInput.toIntOrNull() ?: 0)
                    habitViewModel.addHabit(
                        name = name,
                        description = desc,
                        icon = selectedIcon,
                        color = selectedColor.toInt(),
                        xp = 15,
                        targetDays = finalTarget,
                        reminderTime = reminderTime,
                        isAdaptive = isAdaptive
                    )
                    onSave()
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = name.isNotBlank() && (isInfinite || targetDaysInput.isNotBlank()),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColor))
            ) {
                Text("Создать привычку", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}