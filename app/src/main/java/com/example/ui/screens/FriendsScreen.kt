package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: MainViewModel, onChat: (Long, String) -> Unit) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.example.supabase.Profile>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("好友", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))

        // Search
        OutlinedTextField(
            value = searchQuery, onValueChange = {
                searchQuery = it
                if (it.length >= 2) viewModel.searchUsers(it) { searchResults = it }
                else searchResults = emptyList()
            },
            label = { Text("搜索用户") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        if (searchResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("搜索结果", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            searchResults.forEach { profile ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.sendFriendRequest(profile.id)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.username, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.PersonAdd, "添加好友", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Pending requests
        if (pendingRequests.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("好友请求", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            pendingRequests.forEach { req ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(req.userId, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.acceptFriendRequest(req.userId) }) {
                            Icon(Icons.Default.Check, "接受", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Close, "拒绝", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Friend list
        Spacer(Modifier.height(16.dp))
        Text("好友列表 (${friends.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

        if (friends.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text("还没有好友，搜索用户名添加吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Spacer(Modifier.height(8.dp))
            friends.forEach { friendId ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.startConversation(friendId) { convId, name ->
                            if (convId != null) onChat(convId, name)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(friendId, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

