package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.viewmodel.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val records by viewModel.allTimeRecords.collectAsStateWithLifecycle()
    val todayMinutes = records.filter { System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000 }.sumOf { it.durationMinutes }
    
    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    
    var showPomodoroDialog by remember { mutableStateOf(false) }
    var pomodoroDuration by remember { mutableStateOf(25) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentSecondsLeft by remember { mutableStateOf(1500) }

    // Tick the timer if running (simplified demo tick or fast forward)
    LaunchedEffect(isTimerRunning, currentSecondsLeft) {
        if (isTimerRunning && currentSecondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            currentSecondsLeft--
        } else if (isTimerRunning && currentSecondsLeft == 0) {
            isTimerRunning = false
            // Complete session! Log time: pomodoroDuration minutes
            viewModel.addTimeRecord("Tomato Focus", "Completed $pomodoroDuration min Pomodoro", pomodoroDuration)
            showPomodoroDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header matching Geometric Balance
        Column {
            Text(
                "欢迎来到 CampusAI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "基于柳比歇夫时间统计法与AI的大学生成长社区",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Today's Focus Card (The geometric center)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "今日专注时段 • 柳比歇夫统计",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.25f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${todayMinutes / 60}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "小时 ",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${todayMinutes % 60}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "分钟",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "柳比歇夫时间统计法：精细化自我管理",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
        }

        // AI Insight Card (Geometric Light Purple)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { navController.navigate("analysis") }
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI 匹配与习惯分析：推荐心流",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (aiState.startsWith("Tap") || aiState.contains("Tap")) "今晚您与 'Java/篮球' 学习小组的匹配度达 92%！点击可生成本周 AI 柳比歇夫成长报告。" else aiState,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
        }

        // Quick Action Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pomodoro Action Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        pomodoroDuration = 25
                        currentSecondsLeft = 25 * 60
                        isTimerRunning = false
                        showPomodoroDialog = true
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍅", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("专注番茄钟", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Trade Shortcut Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("barter") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📚", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("二手交易", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Growth Achievements (Recent Badges)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "成长与游戏化成就",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Text(
                        "查看全部",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { navController.navigate("profile") { popUpTo("dashboard") { saveState = true }; launchSingleTop = true; restoreState = true } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BadgeItem("🏆", "连续7天专注", Color(0xFFFFF8E1))
                    BadgeItem("🧘", "禅定模式", Color(0xFFECEFF1))
                    BadgeItem("🥇", "时间掌控者", Color(0xFFE3F2FD))
                }
            }
        }
    }

    if (showPomodoroDialog) {
        AlertDialog(
            onDismissRequest = { showPomodoroDialog = false },
            title = { Text("🍅 专注番茄钟") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isTimerRunning) {
                        val minutes = currentSecondsLeft / 60
                        val seconds = currentSecondsLeft % 60
                        Text(
                            String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { isTimerRunning = false }) {
                                Text("暂停")
                            }
                            Button(
                                onClick = {
                                    // Instantly complete and earn EXP for demo purposes
                                    currentSecondsLeft = 0
                                    isTimerRunning = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("模拟快速完成 (+10 XP)")
                            }
                        }
                    } else {
                        Text("请选择本次专注时长：")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(25, 50, 90).forEach { mins ->
                                FilterChip(
                                    selected = pomodoroDuration == mins,
                                    onClick = {
                                        pomodoroDuration = mins
                                        currentSecondsLeft = mins * 60
                                    },
                                    label = { Text("${mins}分钟") }
                                )
                            }
                        }
                        Text("完成即可获得 10 经验值与成就勋章。")
                        Button(
                            onClick = { isTimerRunning = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("立即开始专注")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPomodoroDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun BadgeItem(emoji: String, title: String, tintColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tintColor),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
        }
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
