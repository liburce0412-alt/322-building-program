package com.example.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 对应 GitHub Raw 托管的 update.json 结构。
 *
 * 示例 JSON：
 * ```json
 * {
 *   "versionCode": 6,
 *   "versionName": "1.5",
 *   "updateLog": "1. 优化了性能\n2. 修复了若干Bug",
 *   "downloadUrl": "https://raw.githubusercontent.com/owner/repo/branch/app-debug.apk",
 *   "forceUpdate": false
 * }
 * ```
 */
@Serializable
data class UpdateInfo(
    @SerialName("versionCode")
    val versionCode: Int,

    @SerialName("versionName")
    val versionName: String,

    @SerialName("updateLog")
    val updateLog: String,

    @SerialName("downloadUrl")
    val downloadUrl: String,

    @SerialName("forceUpdate")
    val forceUpdate: Boolean = false
)
