package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: MainViewModel) {
    LaunchedEffect(Unit) { viewModel.loadPendingMessages(); viewModel.loadPendingChatMessages() }
    val pendingMessages by viewModel.pendingMessages.collectAsStateWithLifecycle()
    val pendingChatMessages by viewModel.pendingChatMessages.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("管理后台", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("留言审核 (${pendingMessages.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("聊天审核 (${pendingChatMessages.size})") })
        }
        Spacer(Modifier.height(12.dp))

        when (tab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingMessages.isEmpty()) {
                    item { Text("暂无待审核留言", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(pendingMessages) { msg ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(msg.username, fontWeight = FontWeight.Bold)
                                Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { viewModel.approveMessage(msg.id) }) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp)); Text("通过")
                                    }
                                    OutlinedButton(onClick = { viewModel.deleteMessage(msg.id) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp)); Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingChatMessages.isEmpty()) {
                    item { Text("暂无待审核聊天消息", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(pendingChatMessages) { msg ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("发送者: ${msg.senderId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { viewModel.approveChatMessage(msg.id) }) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp)); Text("通过")
                                    }
                                    OutlinedButton(onClick = { viewModel.deleteChatMessage(msg.id) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp)); Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
