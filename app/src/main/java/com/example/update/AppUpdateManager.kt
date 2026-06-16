package com.example.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * 应用更新管理器。
 *
 * 职责：
 * 1. 通过 [DownloadManager] 异步下载 APK
 * 2. 下载完成后通过 [FileProvider] + Intent 拉起系统安装界面
 * 3. 自动适配 Android 7.0 ~ 14+ 的权限与安全机制
 *
 * 典型使用流程：
 * ```
 * MainActivity 点击"立即更新" → AppUpdateManager.startDownload(context, url)
 * ```
 * 下载完成后 [UpdateInstallReceiver] 自动触发安装。
 */
object AppUpdateManager {

    private const val FILE_PROVIDER_AUTHORITY = "com.aistudio.campusai.abcxyz.fileprovider"
    private const val APK_FILE_NAME = "app-update.apk"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_DOWNLOAD_ID = "download_id"

    // 动态注册的下载完成接收器
    private var downloadReceiver: BroadcastReceiver? = null

    // ====================================================================
    // 启动下载
    // ====================================================================

    /**
     * 开始下载 APK。
     * 下载到应用的 external files 目录下的 Download/ 子目录。
     */
    fun startDownload(context: Context, downloadUrl: String) {
        // 清除上一次的下载文件
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, APK_FILE_NAME)
        if (apkFile.exists()) {
            apkFile.delete()
        }

        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("校园达人 更新")
            .setDescription("正在下载新版本 APK...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(apkFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        // Android 11+：DownloadManager 需要 MEDIA_LOCAL 或特定 URI
        // 使用 setDestinationUri(Uri.fromFile(...)) 直接写入文件

        val downloadId = downloadManager.enqueue(request)

        // 保存 downloadId 到 SharedPreferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .apply()

        // 动态注册下载完成广播
        registerReceiver(context)
    }

    // ====================================================================
    // 注册 / 注销广播接收器
    // ====================================================================

    private fun registerReceiver(context: Context) {
        if (downloadReceiver != null) return

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
                    val receivedId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, -1L
                    )
                    val savedId = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getLong(KEY_DOWNLOAD_ID, -1L)

                    if (receivedId == savedId && receivedId != -1L) {
                        // 下载完成，触发安装
                        installApk(ctx)
                        // 注销接收器，避免重复触发
                        unregisterReceiver(ctx)
                    }
                }
            }
        }

        context.registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    /**
     * 注销广播接收器，防止内存泄漏。
     */
    fun unregisterReceiver(context: Context) {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
                // receiver 已注销，忽略
            }
            downloadReceiver = null
        }
    }

    // ====================================================================
    // 安装 APK（适配 Android 7.0 ~ 14+）
    // ====================================================================

    /**
     * 通过 FileProvider + Intent 拉起系统安装界面。
     */
    fun installApk(context: Context) {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, APK_FILE_NAME)
        if (!apkFile.exists()) return

        // Android 7.0+：必须使用 FileProvider 生成 content:// URI
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Android 14+ (API 34+)：系统安装器需要在后台启动
            // Intent.FLAG_ACTIVITY_NEW_TASK 已包含此处理
        }

        context.startActivity(installIntent)
    }
}
