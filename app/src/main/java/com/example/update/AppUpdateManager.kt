package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AppUpdateManager {

    private const val FILE_PROVIDER_AUTHORITY = "com.aistudio.campusai.abcxyz.fileprovider"
    private const val APK_FILE_NAME = "app-update.apk"
    private const val MAX_RETRIES = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var appContext: Context? = null

    fun startDownload(context: Context, downloadUrl: String) {
        appContext = context.applicationContext
        scope.launch { downloadApk(context, downloadUrl) }
    }

    private suspend fun downloadApk(context: Context, downloadUrl: String) {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val apkFile = File(downloadDir, APK_FILE_NAME)
        if (apkFile.exists()) apkFile.delete()

        var lastError: Exception? = null

        for (attempt in 1..MAX_RETRIES) {
            try {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(downloadUrl)
                        .header("Cache-Control", "no-cache")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP " + response.code)
                    }

                    val body = response.body ?: throw RuntimeException("Empty body")

                    FileOutputStream(apkFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }

                installApk(context)
                return

            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_RETRIES) {
                    delay(2000L * attempt)
                }
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                appContext ?: context,
                "下载失败，请稍后重试",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun installApk(context: Context) {
        val downloadDir = appContext?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir == null) return

        val apkFile = File(downloadDir, APK_FILE_NAME)
        if (!apkFile.exists()) return

        val apkUri: Uri = FileProvider.getUriForFile(
            appContext ?: context,
            FILE_PROVIDER_AUTHORITY,
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        (appContext ?: context).startActivity(installIntent)
    }
}
