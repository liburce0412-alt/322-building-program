package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.ai.DeepSeekService
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GoodItem
import com.example.data.TimeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import com.example.supabase.ChatMessage
import com.example.supabase.Achievement
import com.example.supabase.Friend
import com.example.supabase.Message
import com.example.supabase.UserAchievement
import com.example.supabase.Profile
import com.example.supabase.SupabaseRepository
import java.io.ByteArrayOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("campus_ai_prefs", Context.MODE_PRIVATE)
    private val supabase = SupabaseRepository
    
    // API key management
    val apiKeyState = MutableStateFlow(prefs.getString("deepseek_api_key", "") ?: "")
    
    init {
        val savedKey = apiKeyState.value
        if (savedKey.isNotEmpty()) {
            com.example.ai.RetrofitClient.apiKey = savedKey
        }
    }
    
    fun saveApiKey(key: String) {
        prefs.edit().putString("deepseek_api_key", key).apply()
        apiKeyState.value = key
        com.example.ai.RetrofitClient.apiKey = key
    }
    
    // Interest tags
    val interestTags = MutableStateFlow(prefs.getString("interest_tags", "") ?: "")
    
    fun addInterestTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        val current = interestTags.value
        val tags = if (current.isEmpty()) trimmed else "$current,$trimmed"
        interestTags.value = tags
        prefs.edit().putString("interest_tags", tags).apply()
    }
    
    fun removeInterestTag(tag: String) {
        val tags = interestTags.value.split(",").filter { it.isNotBlank() && it != tag }
        val result = tags.joinToString(",")
        interestTags.value = result
        prefs.edit().putString("interest_tags", result).apply()
    }

    
    // ==========================================
    // Auth / Profile
    // ==========================================

    val currentUserId = MutableStateFlow("")
    val currentSupabaseUserId = MutableStateFlow("")
    val avatarUrl = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val uid = supabase.getCurrentUserId()
            if (!uid.isNullOrBlank()) {
                currentUserId.value = uid
                currentSupabaseUserId.value = uid
                val profile = supabase.getProfile(uid)
                avatarUrl.value = profile?.avatarUrl
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val uid = currentUserId.value
            if (uid.isBlank()) return@launch
            val profile = supabase.getProfile(uid)
            avatarUrl.value = profile?.avatarUrl
        }
    }

    fun uploadAvatar(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val ctx = getApplication<android.app.Application>()
                val input = ctx.contentResolver.openInputStream(uri) ?: return@launch
                val baos = java.io.ByteArrayOutputStream()
                input.copyTo(baos)
                val bytes = baos.toByteArray()
                val uid = currentUserId.value
                if (uid.isBlank()) return@launch
                val url = supabase.uploadAvatar(uid, bytes)
                if (url != null) {
                    supabase.updateAvatar(uid, url)
                    avatarUrl.value = url
                }
            } catch (_: java.lang.Exception) { }
        }
    }

    // ==========================================
    // Friends
    // ==========================================

    val friends = MutableStateFlow<List<String>>(emptyList())
    val pendingRequests = MutableStateFlow<List<Friend>>(emptyList())

    fun loadFriends() {
        viewModelScope.launch {
            val uid = currentUserId.value
            if (uid.isBlank()) return@launch
            friends.value = supabase.getFriends(uid)
            pendingRequests.value = supabase.getPendingRequests(uid)
        }
    }

    fun searchUsers(query: String, callback: (List<Profile>) -> Unit) {
        viewModelScope.launch {
            callback(supabase.searchUsers(query))
        }
    }

    fun sendFriendRequest(friendId: String) {
        viewModelScope.launch {
            supabase.sendFriendRequest(currentUserId.value, friendId)
            loadFriends()
        }
    }

    fun acceptFriendRequest(friendId: String) {
        viewModelScope.launch {
            supabase.acceptFriendRequest(currentUserId.value, friendId)
            loadFriends()
        }
    }

    fun startConversation(friendId: String, callback: (Long?, String) -> Unit) {
        viewModelScope.launch {
            val convId = supabase.getOrCreateConversation(currentUserId.value, friendId)
            callback(convId, friendId)
        }
    }

    // ==========================================
    // Chat
    // ==========================================

    val chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    fun loadChatMessages(conversationId: Long) {
        viewModelScope.launch {
            chatMessages.value = supabase.getChatMessages(conversationId)
        }
    }

    fun sendChatMessage(conversationId: Long, text: String) {
        viewModelScope.launch {
            supabase.sendChatMessage(conversationId, currentUserId.value, text)
            loadChatMessages(conversationId)
        }
    }


    // ==========================================
    // Achievements
    // ==========================================

    val achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val userAchievements = MutableStateFlow<List<UserAchievement>>(emptyList())

    fun loadAchievements() {
        viewModelScope.launch {
            achievements.value = supabase.getAllAchievements()
            val uid = currentUserId.value
            if (uid.isNotBlank()) {
                userAchievements.value = supabase.getUserAchievements(uid)
            }
        }
    }

    // ==========================================
    // Admin / Moderation
    // ==========================================

    val pendingMessages = MutableStateFlow<List<Message>>(emptyList())
    val pendingChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    fun loadPendingMessages() {
        viewModelScope.launch {
            pendingMessages.value = supabase.getPendingMessages()
        }
    }

    fun loadPendingChatMessages() {
        viewModelScope.launch {
            pendingChatMessages.value = supabase.getPendingChatMessages()
        }
    }

    fun approveMessage(id: Long) {
        viewModelScope.launch { supabase.approveMessage(id); loadPendingMessages() }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch { supabase.deleteMessage(id); loadPendingMessages() }
    }

    fun approveChatMessage(id: Long) {
        viewModelScope.launch { supabase.approveChatMessage(id); loadPendingChatMessages() }
    }

    fun deleteChatMessage(id: Long) {
        viewModelScope.launch { supabase.deleteChatMessage(id); loadPendingChatMessages() }
    }

private val db = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "campus_ai_db"
    ).build()

    private val repository = AppRepository(db.appDao())

    val allTimeRecords: StateFlow<List<TimeRecord>> = repository.allTimeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoods: StateFlow<List<GoodItem>> = repository.allGoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiAnalysisState = MutableStateFlow("点击上方‘一键生成报告’按钮，AI将根据您的柳比歇夫时间记录生成极富洞察力的成长建议。")

    fun addTimeRecord(category: String, description: String, durationMins: Int) {
        viewModelScope.launch {
            repository.insertTimeRecord(
                TimeRecord(
                    category = category,
                    description = description,
                    durationMinutes = durationMins
                )
            )
        }
    }

    fun addGoodItem(title: String, description: String, tags: String) {
        viewModelScope.launch {
            repository.insertGood(
                GoodItem(
                    title = title,
                    description = description,
                    tags = tags
                )
            )
        }
    }

    fun generateAiAnalysis() {
        viewModelScope.launch {
            aiAnalysisState.value = "AI 正在深度分析您的日常习惯与心流时段..."
            // Get records from last 7 days usually, but for demo just get all from state
            val records = allTimeRecords.value
            if (records.isEmpty()) {
                aiAnalysisState.value = "您目前还没有记录任何专注时间。请在‘时间统计’中添加一些专注记录后重试！"
                return@launch
            }
            
            val stats = records.groupBy { it.category }.map { (cat, list) -> 
                 "$cat: ${list.sumOf { it.durationMinutes }} 分钟" 
            }.joinToString("\n")
            
            val details = records.take(15).joinToString("\n") { 
                "- ${it.category}: ${it.durationMinutes}分钟 (${it.description})" 
            }
            
            val promptText = "Summary:\n$stats\n\nRecent Details:\n$details"
            
            val result = DeepSeekService.generateAnalysis(promptText)
            aiAnalysisState.value = result
        }
    }
}
