package com.example.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val username: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Good(
    val id: Long = 0,
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val tags: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Message(
    val id: Long = 0,
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    val content: String = "",
    @SerialName("is_approved") val isApproved: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ChatMessage(
    val id: Long = 0,
    @SerialName("conversation_id") val conversationId: Long = 0,
    @SerialName("sender_id") val senderId: String = "",
    val content: String = "",
    @SerialName("is_approved") val isApproved: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Conversation(
    val id: Long = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ConversationParticipant(
    @SerialName("conversation_id") val conversationId: Long = 0,
    @SerialName("user_id") val userId: String = ""
)

@Serializable
data class Friend(
    @SerialName("user_id") val userId: String = "",
    @SerialName("friend_id") val friendId: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Achievement(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val target: Int = 0,
    val category: String = ""
)

@Serializable
data class UserAchievement(
    @SerialName("user_id") val userId: String = "",
    @SerialName("achievement_id") val achievementId: Long = 0,
    val progress: Int = 0,
    @SerialName("unlocked_at") val unlockedAt: String? = null
)
