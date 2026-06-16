package com.example.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.DownloadManager

/**
 * 独立 BroadcastReceiver，可选地在 AndroidManifest.xml 中注册作为动态注册的补充。
 *
 * 动态注册方案（默认）：
 * - [AppUpdateManager] 在 startDownload() 时动态注册一个 BroadcastReceiver，
 *   接收 [DownloadManager.ACTION_DOWNLOAD_COMPLETE] 后自动触发安装。
 * - 优点是生命周期可控，不需要在 manifest 中暴露。
 *
 * Manifest 注册方案（备选）：
 * 如果需要在 App 被杀死后也能接收下载完成广播，可取消下方注释，
 * 并在 AndroidManifest.xml 中注册此类。
 * 注意：manifest 注册无法收到隐式广播（Android 8.0+ 限制），
 * 但 [DownloadManager.ACTION_DOWNLOAD_COMPLETE] 是系统显式广播，不受此限制。
 *
 * 使用 manifest 方案时，需要在 AndroidManifest.xml 中添加：
 * ```xml
 * <receiver
 *     android:name="com.example.update.UpdateInstallReceiver"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="android.intent.action.DOWNLOAD_COMPLETE" />
 *     </intent-filter>
 * </receiver>
 * ```
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
            val receivedId = intent.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID, -1L
            )
            val savedId = context.getSharedPreferences(
                "app_update_prefs", Context.MODE_PRIVATE
            ).getLong("download_id", -1L)

            if (receivedId == savedId && receivedId != -1L) {
                AppUpdateManager.installApk(context)
            }
        }
    }
}
