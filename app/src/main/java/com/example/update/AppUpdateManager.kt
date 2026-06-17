package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // Progress state exposed to UI
    private val _downloadProgress = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadProgress: StateFlow<DownloadState> = _downloadProgress

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var appContext: Context? = null

    fun startDownload(context: Context, updateInfo: UpdateInfo) {
        appContext = context.applicationContext
        _downloadProgress.value = DownloadState.Preparing
        scope.launch { downloadApk(context, updateInfo) }
    }

    private suspend fun downloadApk(context: Context, updateInfo: UpdateInfo) {
        val downloadUrl = updateInfo.downloadUrl
        Log.d("[DOWNLOAD]", "Starting download from: $downloadUrl")

        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val apkFile = File(downloadDir, APK_FILE_NAME)
        if (apkFile.exists()) {
            Log.d("[DOWNLOAD]", "Removing previous APK: ${apkFile.length()} bytes")
            apkFile.delete()
        }

        // Build URL list: primary + dynamically constructed fallback
        val urls = buildUrlList(downloadUrl)

        for ((urlIndex, url) in urls.withIndex()) {
            for (attempt in 1..MAX_RETRIES) {
                Log.d("[DOWNLOAD]", "Trying URL #${urlIndex + 1} attempt #$attempt: $url")
                _downloadProgress.value = DownloadState.Connecting(urlIndex + 1, attempt)

                try {
                    withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:120.0)")
                            .header("Accept", "application/vnd.android.package-archive")
                            .build()

                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) {
                            val code = response.code
                            response.close()
                            Log.w("[DOWNLOAD]", "HTTP $code for $url")
                            throw RuntimeException("HTTP $code")
                        }

                        val body = response.body ?: throw RuntimeException("Empty body")
                        val contentLength = body.contentLength()
                        Log.d("[DOWNLOAD]", "Content-Length: $contentLength")
                        _downloadProgress.value = DownloadState.Downloading(0, contentLength)

                        var totalRead = 0L
                        var lastProgressUpdate = 0L

                        FileOutputStream(apkFile).use { output ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalRead += bytesRead
                                    // Throttle progress updates to prevent UI overload
                                    if (totalRead - lastProgressUpdate > 65536 || totalRead == contentLength) {
                                        lastProgressUpdate = totalRead
                                        val progress = if (contentLength > 0) {
                                            DownloadState.Downloading(totalRead, contentLength)
                                        } else {
                                            DownloadState.Downloading(totalRead, -1L)
                                        }
                                        _downloadProgress.value = progress
                                    }
                                }
                            }
                        }
                        Log.d("[APK]", "Downloaded ${apkFile.length()} bytes to ${apkFile.absolutePath}")
                    }

                    // Verify downloaded APK
                    _downloadProgress.value = DownloadState.Verifying
                    val isValid = verifyApk(context, apkFile, updateInfo)
                    if (isValid) {
                        Log.d("[APK]", "APK verification passed")
                        _downloadProgress.value = DownloadState.Ready
                        installApk(context)
                        return
                    } else {
                        Log.e("[APK]", "APK verification failed, will retry")
                        apkFile.delete()
                        throw RuntimeException("APK verification failed")
                    }

                } catch (e: Exception) {
                    Log.e("[DOWNLOAD]", "Attempt $attempt failed: ${e.message}")
                    if (attempt < MAX_RETRIES) {
                        val delayMs = 2000L * attempt
                        Log.d("[DOWNLOAD]", "Retrying in ${delayMs}ms...")
                        delay(delayMs)
                    }
                }
            }
        }

        // All URLs and attempts exhausted
        Log.e("[DOWNLOAD]", "All download URLs exhausted")
        _downloadProgress.value = DownloadState.Failed
        withContext(Dispatchers.Main) {
            Toast.makeText(
                appContext ?: context,
                "下载失败，请稍后重试",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildUrlList(downloadUrl: String): List<String> {
        val urls = mutableListOf(downloadUrl)

        // Dynamically construct fallback Raw URL from any Gitee Release URL
        val rawMatch = Regex("""releases/download/v?[\d.]+/""").find(downloadUrl)
        if (rawMatch != null) {
            val rawUrl = downloadUrl.replace(rawMatch.value, "raw/master/")
            if (rawUrl != downloadUrl) {
                Log.d("[DOWNLOAD]", "Fallback URL constructed: $rawUrl")
                urls.add(rawUrl)
            }
        }

        return urls
    }

    private fun verifyApk(context: Context, apkFile: File, expected: UpdateInfo): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e("[APK]", "APK file missing or empty")
            return false
        }

        val apkSize = apkFile.length()
        Log.d("[APK]", "APK file size: $apkSize bytes")

        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (pkgInfo == null) {
                Log.e("[APK]", "getPackageArchiveInfo returned null — corrupted or incomplete APK")
                return false
            }
            val apkVersionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                pkgInfo.longVersionCode
            } else {
                pkgInfo.versionCode.toLong()
            }
            val apkVersionName = pkgInfo.versionName ?: "unknown"
            val apkPackageName = pkgInfo.packageName

            Log.d("[APK]", "Downloaded APK: $apkPackageName v$apkVersionName ($apkVersionCode)")
            Log.d("[APK]", "Expected: ${expected.versionName} (${expected.versionCode})")

            // Verify package name matches
            val ourPkg = context.packageName
            if (apkPackageName != ourPkg) {
                Log.e("[APK]", "Package mismatch: downloaded=$apkPackageName, expected=$ourPkg")
                return false
            }

            // Verify version code is higher than current
            if (apkVersionCode <= expected.versionCode - 1) {
                Log.w("[APK]", "Downloaded APK version ($apkVersionCode) is not the expected update (${expected.versionCode})")
                // Still allow installation if it's higher than current
                val currentVersionCode = try {
                    val ctx = appContext ?: context
                    val currentInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                    if (android.os.Build.VERSION.SDK_INT >= 28) currentInfo.longVersionCode
                    else currentInfo.versionCode.toLong()
                } catch (e: Exception) {
                    0L
                }
                if (apkVersionCode <= currentVersionCode) {
                    Log.e("[APK]", "Downloaded APK ($apkVersionCode) <= current ($currentVersionCode), rejecting")
                    return false
                }
                Log.w("[APK]", "Downloaded APK is newer than current but may not be the expected version")
            }

            true
        } catch (e: Exception) {
            Log.e("[APK]", "Verification exception", e)
            false
        }
    }

    private fun installApk(context: Context) {
        Log.d("[INSTALL]", "Starting installation...")
        _downloadProgress.value = DownloadState.Installing

        val downloadDir = appContext?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir == null) {
            Log.e("[INSTALL]", "Download directory is null")
            return
        }

        val apkFile = File(downloadDir, APK_FILE_NAME)
        if (!apkFile.exists()) {
            Log.e("[INSTALL]", "APK file not found: ${apkFile.absolutePath}")
            return
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            appContext ?: context,
            FILE_PROVIDER_AUTHORITY,
            apkFile
        )
        Log.d("[INSTALL]", "FileProvider URI: $apkUri")

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            (appContext ?: context).startActivity(installIntent)
            Log.d("[INSTALL]", "Install intent dispatched successfully")
        } catch (e: Exception) {
            Log.e("[INSTALL]", "Failed to start install intent", e)
            _downloadProgress.value = DownloadState.Failed
            Toast.makeText(
                appContext ?: context,
                "无法启动安装，请检查安装权限",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun resetProgress() {
        _downloadProgress.value = DownloadState.Idle
    }
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Preparing : DownloadState()
    data class Connecting(val urlIndex: Int, val attempt: Int) : DownloadState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data object Verifying : DownloadState()
    data object Ready : DownloadState()
    data object Installing : DownloadState()
    data object Failed : DownloadState()
}
