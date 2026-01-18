package com.example.habittracker.ui.theme.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.* import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.* import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.ui.theme.viewmodel.HabitListViewModel
import com.example.habittracker.ui.theme.viewmodel.HabitViewModel
import com.example.habittracker.util.StringUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun HabitListScreen(
    viewModel: HabitListViewModel,
    habitViewModel: HabitViewModel,
    onAddHabit: () -> Unit,
    onSettings: () -> Unit,
    onArchiveClick: () -> Unit,
    onHabitClick: (String) -> Unit
) {
    val habits by viewModel.habits.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }


    LaunchedEffect(key1 = true) {
        viewModel.updateTodayStatus()
    }

    //Слушатель жизненного цикла приложения
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateTodayStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                viewModel.updateTodayStatus()
                delay(800)
                refreshing = false
            }
        }
    )

    //Перехватываем
    BackHandler(enabled = true) {
        if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else {
            (context as? Activity)?.finish()
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    if (isSearching) {
                        androidx.compose.material3.TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск привычки...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Column {
                            val remaining = habits.count { !todayCompletions.contains(it.id) }
                            Text("Мой Сад", fontWeight = FontWeight.Black, fontSize = 24.sp)
                            Text(
                                text = if (remaining > 0) StringUtils.formatHabitsLeft(remaining) else "Все цели достигнуты!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, contentDescription = "Поиск")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Аналитика и архив") },
                                onClick = {
                                    showMenu = false
                                    onArchiveClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Analytics, null) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    showMenu = false
                                    onSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить привычку")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    LevelProgressCard(stats.level, stats.totalXp)
                }

                item {
                    TipsPager()
                }

                val activeHabits = habits.filter { !it.isArchived }
                val filteredHabits = activeHabits.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                if (activeHabits.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌱 Пока здесь ничего не растет", color = Color.Gray)
                        }
                    }
                } else if (filteredHabits.isEmpty() && isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ничего не найдено", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredHabits, key = { it.id }) { habit ->
                        val dismissState = androidx.compose.material.rememberDismissState(
                            confirmStateChange = {
                                if (it == androidx.compose.material.DismissValue.DismissedToStart) {
                                    habitViewModel.archiveHabit(habit.id, true)
                                    true
                                } else false
                            }
                        )

                        androidx.compose.material.SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(androidx.compose.material.DismissDirection.EndToStart),
                            background = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFE57373)),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        modifier = Modifier.padding(end = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("В архив", color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.Archive, null, tint = Color.White)
                                    }
                                }
                            },
                            dismissContent = {
                                HabitItem(
                                    habit = habit,
                                    isCompletedToday = todayCompletions.contains(habit.id),
                                    // ИСПРАВЛЕНО: Передаем объект habit целиком
                                    onToggle = { viewModel.toggleHabit(habit) },
                                    onClick = { onHabitClick(habit.id) }
                                )
                            }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TipsPager() {
    val tips = listOf(
        "🌱 Маленькие шаги" to "Дерево растет медленно, но верно. Начните с 5 минут в день для формирования привычки.",
        "💧 Регулярность" to "Не забывайте поливать свои привычки. Постоянство важнее интенсивности нагрузки.",
        "🧘 Пауза" to "Сделайте глубокий вдох. Иногда отдых — это самый быстрый путь к вашей цели.",
        "📚 Развитие" to "Чтение всего 10 страниц в день превращается в 12 прочитанных книг за один год!"
    )
    val pagerState = rememberPagerState(pageCount = { tips.size })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp
    ) { page ->
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 32.sp)
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(tips[page].first, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        tips[page].second,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LevelProgressCard(level: Int, xp: Int) {
    val nextLevelXp = 100 + (level * 50)
    val progress = (xp.toFloat() / nextLevelXp).coerceIn(0f, 1f)
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.1f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Уровень $level", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("Ваш сад процветает", style = MaterialTheme.typography.bodySmall)
                }
                Text("$xp / $nextLevelXp XP", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(0.2f)
            )
        }
    }
}

@Composable
fun HabitItem(
    habit: Habit,
    isCompletedToday: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val cardColor = if (isCompletedToday) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.0f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = cardColor
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(Color(habit.color))
            )

            Row(
                Modifier
                    .padding(16.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(habit.icon, fontSize = 28.sp)

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "Стрик: ${habit.currentStreak} ${StringUtils.getRussianPlural(habit.currentStreak, "день", "дня", "дней")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isCompletedToday) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isCompletedToday) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isCompletedToday) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}