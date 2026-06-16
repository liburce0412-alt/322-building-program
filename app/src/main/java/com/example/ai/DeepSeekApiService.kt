package com.example.ai

// API key loaded at runtime (set via MainViewModel/SharedPreferences)
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String = "deepseek-chat",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ChatMessage
)

interface DeepSeekApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

object RetrofitClient {
    /** Dynamic API key - set at runtime via user input or fallback to BuildConfig */
    var apiKey: String = ""

    private const val BASE_URL = "https://api.deepseek.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        })
        .build()

    val service: DeepSeekApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(DeepSeekApiService::class.java)
    }
}

object DeepSeekService {
    suspend fun generateAnalysis(recordsText: String): String = withContext(Dispatchers.IO) {
        val systemPrompt = "你是 CampusAI，一位乐于助人、鼓励人心的大学生 AI 助手。请基于柳比歇夫时间记录法，为用户提供有洞察力的时间管理反馈。必须用中文回复。"

        val userPrompt = "以下是一名大学生的近期时间记录，请提供一份简短、鼓励性的成长分析报告和个性化建议。用纯文本返回，不要使用 Markdown 格式。\n记录内容：\n$recordsText"

        val request = ChatCompletionRequest(
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )
        try {
            val response = RetrofitClient.service.chatCompletion(request)
            response.choices.firstOrNull()?.message?.content
                ?: "目前没有分析建议，继续记录您的时间吧！"
        } catch (e: Exception) {
            "分析数据时出错：${e.localizedMessage}。请重试。"
        }
    }
}
