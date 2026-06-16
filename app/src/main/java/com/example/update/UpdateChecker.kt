package com.example.update

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object UpdateChecker {

    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/liburce0412-alt/322-building-program/master/update.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_POSTPONE_UNTIL = "postpone_until"

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val postponeUntil = prefs.getLong(KEY_POSTPONE_UNTIL, 0L)
        if (System.currentTimeMillis() < postponeUntil) {
            return@withContext null
        }

        val request = Request.Builder()
            .url(UPDATE_JSON_URL)
            .header("Cache-Control", "no-cache")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return@withContext null
        }

        val body = response.body?.string() ?: return@withContext null

        val updateInfo = try {
            json.decodeFromString<UpdateInfo>(body)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        val currentVersionCode = BuildConfig.VERSION_CODE
        if (updateInfo.versionCode > currentVersionCode) {
            return@withContext updateInfo
        }

        null
    }

    fun postpone(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextCheck = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        prefs.edit().putLong(KEY_POSTPONE_UNTIL, nextCheck).apply()
    }

    fun clearPostpone(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_POSTPONE_UNTIL).apply()
    }
}
