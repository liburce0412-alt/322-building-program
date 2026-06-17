package com.example.supabase

import com.example.BuildConfig
import com.example.supabase.Supabase.supabaseUrl
import com.example.supabase.Supabase.supabaseKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val jsonMedia = "application/json".toMediaType()
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build()
private val JsonParser = Json { ignoreUnknownKeys = true }

private var _accessToken: String? = null

fun initSession(accessToken: String?) { _accessToken = accessToken }
fun getStoredToken(): String? = _accessToken
private fun authHeader(): String = "Bearer " + (_accessToken ?: BuildConfig.SUPABASE_ANON_KEY)

private suspend fun supabasePost(path: String, body: String): okhttp3.Response = withContext(Dispatchers.IO) {
    httpClient.newCall(Request.Builder()
        .url("${BuildConfig.SUPABASE_URL}$path")
        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
        .addHeader("Authorization", authHeader())
        .addHeader("Content-Type", "application/json")
        .post(body.toRequestBody(jsonMedia)).build()
    ).execute()
}

private suspend fun supabaseGet(path: String): okhttp3.Response = withContext(Dispatchers.IO) {
    httpClient.newCall(Request.Builder()
        .url("${BuildConfig.SUPABASE_URL}$path")
        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
        .addHeader("Authorization", authHeader()).build()
    ).execute()
}

object SupabaseRepository {

    private suspend fun supabasePatch(path: String, body: String): okhttp3.Response = withContext(Dispatchers.IO) {
        httpClient.newCall(Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}$path")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", authHeader())
            .addHeader("Content-Type", "application/json")
            .patch(body.toRequestBody(jsonMedia)).build()
        ).execute()
    }

    private suspend fun supabaseDelete(path: String): okhttp3.Response = withContext(Dispatchers.IO) {
        httpClient.newCall(Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}$path")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", authHeader())
            .delete().build()
        ).execute()
    }


    // ─── Auth (REST API) ───
    suspend fun signUp(email: String, password: String, username: String): Boolean {
        if (!Supabase.isConfigured) return false
        return try {
            val resp = supabasePost("/auth/v1/signup", """{"email":"$email","password":"$password","data":{"username":"$username"}}""")
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return false
                val obj = JsonParser.parseToJsonElement(body).jsonObject
                val uid = obj["id"]?.jsonPrimitive?.content
                    ?: obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                    ?: return false
                createProfile(uid, username)
                true
            } else {
                // Capture and store the error for display
                val errBody = resp.body?.string() ?: "no body"
                _lastAuthError.value = "HTTP ${resp.code}: $errBody"
                false
            }
        } catch (e: Exception) {
            _lastAuthError.value = "Exception: ${e.message}"
            false
        }
    }

    suspend fun signIn(email: String, password: String): String? {
        if (!Supabase.isConfigured) return null
        return try {
            val resp = supabasePost("/auth/v1/token?grant_type=password", """{"email":"$email","password":"$password"}""")
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return null
                val obj = JsonParser.parseToJsonElement(body).jsonObject
                _accessToken = obj["access_token"]?.jsonPrimitive?.content
                obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content
            } else {
                val errBody = resp.body?.string() ?: "no body"
                _lastAuthError.value = "HTTP ${resp.code}: $errBody"
                null
            }
        } catch (e: Exception) {
            _lastAuthError.value = "Exception: ${e.message}"
            null
        }
    }

    suspend fun signOut() {
        try { supabasePost("/auth/v1/logout", "") } catch (_: Exception) {}
        _accessToken = null
    }

    suspend fun getCurrentUserId(): String? {
        return try {
            val resp = supabaseGet("/auth/v1/user")
            if (resp.isSuccessful) {
                resp.body?.string()?.let { body ->
                    JsonParser.parseToJsonElement(body).jsonObject["id"]?.jsonPrimitive?.content
                }
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun getCurrentUserEmail(): String? {
        return try {
            val resp = supabaseGet("/auth/v1/user")
            if (resp.isSuccessful) {
                resp.body?.string()?.let { body ->
                    JsonParser.parseToJsonElement(body).jsonObject["email"]?.jsonPrimitive?.content
                }
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun isConfigured(): Boolean = Supabase.isConfigured

    private val _lastAuthError = kotlinx.coroutines.flow.MutableStateFlow("")
    val lastAuthError: kotlinx.coroutines.flow.StateFlow<String> get() = _lastAuthError

    // ─── Profile ───
    private suspend fun createProfile(userId: String, username: String) {
        try { supabasePost("/rest/v1/profiles", """{"id":"$userId","username":"$username"}""") } catch (_: Exception) {}
    }

    suspend fun getProfile(userId: String): Profile? = try {
        JsonParser.decodeFromString<List<Profile>>(supabaseGet("/rest/v1/profiles?id=eq.$userId&limit=1").body?.string() ?: "[]").firstOrNull()
    } catch (_: Exception) { null }

    suspend fun updateAvatar(userId: String, url: String) {
        try { supabasePatch("/rest/v1/profiles?id=eq.$userId", """{"avatar_url":"$url"}""") } catch (_: Exception) {}
    }

    suspend fun searchUsers(query: String): List<Profile> = try {
        JsonParser.decodeFromString<List<Profile>>(supabaseGet("/rest/v1/profiles?username=ilike.*$query*&limit=20").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    // ─── Goods ───
    suspend fun getAllGoods(): List<Good> = try {
        JsonParser.decodeFromString<List<Good>>(supabaseGet("/rest/v1/goods?order=created_at.desc").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    suspend fun addGood(userId: String, title: String, desc: String?, tags: String?) {
        try { supabasePost("/rest/v1/goods", """{"user_id":"$userId","title":"$title","description":"${desc ?: ""}","tags":"${tags ?: ""}"}""") } catch (_: Exception) {}
    }

    // ─── Messages ───
    suspend fun getApprovedMessages(): List<Message> = try {
        JsonParser.decodeFromString<List<Message>>(supabaseGet("/rest/v1/messages?is_approved=eq.true&order=created_at.desc&limit=50").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    suspend fun addMessage(userId: String, username: String, content: String) {
        try { supabasePost("/rest/v1/messages", """{"user_id":"$userId","username":"$username","content":"$content","is_approved":false}""") } catch (_: Exception) {}
    }

    suspend fun getPendingMessages(): List<Message> = try {
        JsonParser.decodeFromString<List<Message>>(supabaseGet("/rest/v1/messages?is_approved=eq.false&order=created_at.desc").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    suspend fun approveMessage(id: Long) {
        try { supabasePatch("/rest/v1/messages?id=eq.$id", """{"is_approved":true}""") } catch (_: Exception) {}
    }

    suspend fun deleteMessage(id: Long) {
        try { supabaseDelete("/rest/v1/messages?id=eq.$id") } catch (_: Exception) {}
    }

    // ─── Conversations & Chat ───
    suspend fun getOrCreateConversation(user1: String, user2: String): Long? {
        return try {
            val parts = JsonParser.decodeFromString<List<ConversationParticipant>>(supabaseGet("/rest/v1/conversation_participants?user_id=eq.$user1").body?.string() ?: "[]")
            for (p in parts) {
                val other = JsonParser.decodeFromString<List<ConversationParticipant>>(supabaseGet("/rest/v1/conversation_participants?conversation_id=eq.${p.conversationId}&user_id=eq.$user2&limit=1").body?.string() ?: "[]")
                if (other.isNotEmpty()) return p.conversationId
            }
            val conv = JsonParser.decodeFromString<List<Conversation>>(supabasePost("/rest/v1/conversations", """{}""").body?.string() ?: "[]").first()
            supabasePost("/rest/v1/conversation_participants", """{"conversation_id":${conv.id},"user_id":"$user1"}""")
            supabasePost("/rest/v1/conversation_participants", """{"conversation_id":${conv.id},"user_id":"$user2"}""")
            conv.id
        } catch (_: Exception) { null }
    }

    suspend fun getChatMessages(convId: Long): List<ChatMessage> = try {
        JsonParser.decodeFromString<List<ChatMessage>>(supabaseGet("/rest/v1/chat_messages?conversation_id=eq.$convId&is_approved=eq.true&order=created_at.asc&limit=200").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    suspend fun sendChatMessage(convId: Long, senderId: String, content: String) {
        try { supabasePost("/rest/v1/chat_messages", """{"conversation_id":$convId,"sender_id":"$senderId","content":"$content","is_approved":true}""") } catch (_: Exception) {}
    }

    suspend fun getPendingChatMessages(): List<ChatMessage> = try {
        JsonParser.decodeFromString<List<ChatMessage>>(supabaseGet("/rest/v1/chat_messages?is_approved=eq.false&order=created_at.desc").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    suspend fun approveChatMessage(id: Long) {
        try { supabasePatch("/rest/v1/chat_messages?id=eq.$id", """{"is_approved":true}""") } catch (_: Exception) {}
    }

    suspend fun deleteChatMessage(id: Long) {
        try { supabaseDelete("/rest/v1/chat_messages?id=eq.$id") } catch (_: Exception) {}
    }

    // ─── Friends ───
    suspend fun sendFriendRequest(userId: String, friendId: String) {
        try { supabasePost("/rest/v1/friends", """{"user_id":"$userId","friend_id":"$friendId","status":"pending"}""") } catch (_: Exception) {}
    }

    suspend fun acceptFriendRequest(userId: String, friendId: String) {
        try {
            supabasePatch("/rest/v1/friends?user_id=eq.$friendId&friend_id=eq.$userId", """{"status":"accepted"}""")
            supabasePost("/rest/v1/friends", """{"user_id":"$userId","friend_id":"$friendId","status":"accepted"}""")
        } catch (_: Exception) {}
    }

    suspend fun getFriends(userId: String): List<String> = try {
        val s = JsonParser.decodeFromString<List<Friend>>(supabaseGet("/rest/v1/friends?user_id=eq.$userId&status=eq.accepted").body?.string() ?: "[]").map { it.friendId }
        val r = JsonParser.decodeFromString<List<Friend>>(supabaseGet("/rest/v1/friends?friend_id=eq.$userId&status=eq.accepted").body?.string() ?: "[]").map { it.userId }
        s + r
    } catch (_: Exception) { emptyList() }

    suspend fun getPendingRequests(userId: String): List<Friend> = try {
        JsonParser.decodeFromString<List<Friend>>(supabaseGet("/rest/v1/friends?friend_id=eq.$userId&status=eq.pending").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    // ─── Achievements ───
    suspend fun getAllAchievements(): List<Achievement> = try { JsonParser.decodeFromString<List<Achievement>>(supabaseGet("/rest/v1/achievements").body?.string() ?: "[]") } catch (_: Exception) { emptyList() }

    suspend fun getUserAchievements(userId: String): List<UserAchievement> = try {
        JsonParser.decodeFromString<List<UserAchievement>>(supabaseGet("/rest/v1/user_achievements?user_id=eq.$userId").body?.string() ?: "[]")
    } catch (_: Exception) { emptyList() }

    suspend fun upsertAchievement(userId: String, achievementId: Long, progress: Int, unlocked: String?) {
        try {
            val existing = JsonParser.decodeFromString<List<UserAchievement>>(supabaseGet("/rest/v1/user_achievements?user_id=eq.$userId&achievement_id=eq.$achievementId&limit=1").body?.string() ?: "[]")
            val data = mutableMapOf<String, Any>("progress" to progress)
            if (unlocked != null) data["unlocked_at"] = unlocked
            if (existing.isEmpty()) {
                data["user_id"] = userId; data["achievement_id"] = achievementId
                supabasePost("/rest/v1/user_achievements", JsonParser.encodeToString(kotlinx.serialization.serializer<Map<String, kotlin.Any>>(), data))
            } else {
                supabasePatch("/rest/v1/user_achievements?user_id=eq.$userId&achievement_id=eq.$achievementId", JsonParser.encodeToString(kotlinx.serialization.serializer<Map<String, kotlin.Any>>(), data))
            }
        } catch (_: Exception) {}
    }

    // ─── Storage ───
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String? {
        return try {
            val path = "avatars/$userId.jpg"
            withContext(Dispatchers.IO) {
                httpClient.newCall(okhttp3.Request.Builder()
                    .url("$supabaseUrl/storage/v1/object/avatars/$path")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .addHeader("Content-Type", "application/octet-stream")
                    .put(imageBytes.toRequestBody("application/octet-stream".toMediaType()))
                    .build()
                ).execute()
            }
            "$supabaseUrl/storage/v1/object/public/avatars/$path"
        } catch (_: Exception) { null }
    }
}








