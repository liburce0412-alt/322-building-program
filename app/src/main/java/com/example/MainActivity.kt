package com.example

import android.content.Context
import android.os.Bundle
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.screens.LoginScreen
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.update.DownloadState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Notifications
import androidx.lifecycle.lifecycleScope
import com.example.update.UpdateChecker
import com.example.update.UpdateDialog
import com.example.update.AppUpdateManager
import com.example.update.UpdateInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ---- 更新弹窗状态 ----
        var showUpdateDialog by mutableStateOf(false)
        var updateInfo by mutableStateOf<UpdateInfo?>(null)

        // 在 Activity 生命周期中异步检查更新
        lifecycleScope.launch {
            val result = UpdateChecker.checkForUpdate(this@MainActivity)
            if (result != null) {
                updateInfo = result
                showUpdateDialog = true
            }
}

                val activity = this@MainActivity

        setContent {
            MyApplicationTheme {
                val authViewModel: MainViewModel = viewModel()
                var isLoggedIn by remember { mutableStateOf(false) }
                var authChecked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // Restore saved token from SharedPreferences
                    val prefs = activity.getSharedPreferences("campus_ai_prefs", Context.MODE_PRIVATE)
                    val savedToken = prefs.getString("supabase_access_token", null)
                    if (!savedToken.isNullOrBlank()) {
                        com.example.supabase.initSession(savedToken)
                    }

                    val uid = com.example.supabase.SupabaseRepository.getCurrentUserId()
                    if (uid != null) {
                        authViewModel.currentUserId.value = uid
                        authViewModel.currentSupabaseUserId.value = uid
                        authViewModel.refreshProfile()
                        authViewModel.loadFriends()
                        isLoggedIn = true
                    }
                    authChecked = true
                }

                if (!authChecked) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@MyApplicationTheme
                }

                if (!isLoggedIn) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            // Save token after successful login
                            val prefs = activity.getSharedPreferences("campus_ai_prefs", Context.MODE_PRIVATE)
                            val token = com.example.supabase.getStoredToken()
                            if (!token.isNullOrBlank()) {
                                prefs.edit().putString("supabase_access_token", token).apply()
                            }
                            isLoggedIn = true
                        }
                    )
                    return@MyApplicationTheme
                }

                val viewModel: MainViewModel = authViewModel
                val navController = rememberNavController()

                val items = listOf(
                    "dashboard" to Pair("首页", Icons.Default.Home),
                    "tracker" to Pair("时间统计", Icons.Default.AccessTime),
                    "analysis" to Pair("AI分析", Icons.Default.AutoGraph),
                    "barter" to Pair("闲置交易", Icons.Default.ShoppingCart),
                    "profile" to Pair("个人中心", Icons.Default.Person)
                )

                val records by viewModel.allTimeRecords.collectAsStateWithLifecycle()
                val totalTime = records.sumOf { it.durationMinutes }
                val level = (totalTime / 60) + 1
                val levelTitle = when {
                    level >= 10 -> "LV.$level 柳比歇夫时间大师"
                    level >= 5 -> "LV.$level 专注学者"
                    else -> "LV.$level 时间管理新手"
                }

                val appVersion = BuildConfig.VERSION_NAME

                val downloadState by AppUpdateManager.downloadProgress.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { (route, labelIcon) ->
                                val (label, icon) = labelIcon
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedBackground()
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "C",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Column {
                                        Text(
                                            "校园达人",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            levelTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            appVersion,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "消息通知",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = "dashboard",
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable("dashboard") { DashboardScreen(viewModel, navController) }
                                    composable("tracker") { TimeTrackerScreen(viewModel) }
                                    composable("analysis") { AiAnalysisScreen(viewModel) }
                                    composable("barter") { BarterScreen(viewModel) }
                                    composable("profile") { ProfileScreen(viewModel) }
                                }
                            }
                        }
                    }
                }

                // ---- 弹出更新对话框 ----
                updateInfo?.let { info ->
                    if (showUpdateDialog) {
                        UpdateDialog(
                            updateInfo = info,
                            downloadState = downloadState,
                            onDismiss = {
                                showUpdateDialog = false
                                UpdateChecker.postpone(this@MainActivity)
                            },
                            onUpdate = {
                                showUpdateDialog = false
                                AppUpdateManager.startDownload(
                                    this@MainActivity,
                                    info
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}








