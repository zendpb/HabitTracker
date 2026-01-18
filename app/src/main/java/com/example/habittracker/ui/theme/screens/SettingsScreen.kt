package com.example.habittracker.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.ui.theme.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsVm: SettingsViewModel,
    onOpenAnalytics: () -> Unit,
    onBack: () -> Unit
) {
    val isDarkTheme by settingsVm.isDarkTheme.collectAsState()
    val isAdminMode by settingsVm.isAdminMode.collectAsState()
    val notificationsEnabled by settingsVm.notificationsEnabled.collectAsState()
    val userStats by settingsVm.userStats.collectAsState()

    // Лончер для создания файла (Экспорт)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { settingsVm.exportBackup(it) }
    }

    // Лончер для выбора файла (Импорт)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { settingsVm.importBackup(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ГРУППА: ДАННЫЕ
            SettingsGroup(title = "Данные") {
                SettingsClickableItem(
                    icon = Icons.Default.Analytics,
                    title = "Статистика и аналитика",
                    subtitle = "Графики прогресса и достижений",
                    onClick = onOpenAnalytics,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                // КНОПКА ЭКСПОРТА
                SettingsClickableItem(
                    icon = Icons.Default.FileUpload,
                    title = "Экспорт данных",
                    subtitle = "Сохранить привычки в JSON файл",
                    onClick = { exportLauncher.launch("habit_tracker_backup.json") },
                    color = Color(0xFF4CAF50)
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                // КНОПКА ИМПОРТА
                SettingsClickableItem(
                    icon = Icons.Default.FileDownload,
                    title = "Импорт данных",
                    subtitle = "Восстановить из файла резервной копии",
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    color = Color(0xFF2196F3)
                )
            }

            // ГРУППА: ИНТЕРФЕЙС
            SettingsGroup(title = "Внешний вид") {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Темная тема",
                    subtitle = "Меняет оформление приложения",
                    checked = isDarkTheme,
                    onCheckedChange = { settingsVm.toggleDarkTheme(it) }
                )
            }

            // ГРУППА: АДМИН ПАНЕЛЬ (Показывается только если включен режим разраба)
            if (isAdminMode) {
                SettingsGroup(title = "Параметры разработчика") {
                    // КНОПКА ТЕСТОВОГО УВЕДОМЛЕНИЯ
                    SettingsClickableItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Тестовое уведомление",
                        subtitle = "Проверить работу уведомлений мгновенно",
                        onClick = { settingsVm.sendTestNotification() },
                        color = Color(0xFFE91E63)
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    AdminStatEditor(
                        icon = Icons.Default.Star,
                        title = "Изменить XP",
                        currentValue = userStats.totalXp.toString(),
                        onSave = { newValue ->
                            settingsVm.updateXP(newValue.toIntOrNull() ?: 0)
                        }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    AdminStatEditor(
                        icon = Icons.Default.TrendingUp,
                        title = "Изменить Уровень",
                        currentValue = userStats.level.toString(),
                        onSave = { newValue ->
                            settingsVm.updateLevel(newValue.toIntOrNull() ?: 1)
                        }
                    )
                }
            }

            // ГРУППА: СИСТЕМА
            SettingsGroup(title = "Система") {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления",
                    checked = notificationsEnabled,
                    onCheckedChange = { settingsVm.toggleNotifications(it) }
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                SettingsToggleItem(
                    icon = Icons.Default.Build,
                    title = "Режим разработчика",
                    subtitle = "Доступ к ручному управлению XP",
                    checked = isAdminMode,
                    onCheckedChange = { settingsVm.toggleAdminMode(it) }
                )
            }

            // ГРУППА: ИНФО
            SettingsGroup(title = "О приложении") {
                SettingsInfoItem(
                    icon = Icons.Default.Info,
                    title = "Версия",
                    value = "2.4"
                )
            }
        }
    }
}

@Composable
fun AdminStatEditor(
    icon: ImageVector,
    title: String,
    currentValue: String,
    onSave: (String) -> Unit
) {
    var textValue by remember(currentValue) { mutableStateOf(currentValue) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.height(52.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { onSave(textValue) },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text("OK")
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun SettingsInfoItem(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}