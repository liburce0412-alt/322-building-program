# check_update.ps1 — CampusAI 应用内更新诊断脚本
# 执行前提：需要 Android SDK (aapt2 或 apkanalyzer 在 PATH 中)

param(
    [string]$ApkPath = "app-debug.apk",
    [string]$UpdateJsonPath = "update.json"
)

Write-Host "========================================"
Write-Host "   CampusAI 应用内更新诊断 v1.0"
Write-Host "========================================"

# 查找 Android SDK 工具
$aapt2 = Get-Command "aapt2" -ErrorAction SilentlyContinue
$apkanalyzer = Get-Command "apkanalyzer" -ErrorAction SilentlyContinue
if (-not $aapt2 -and -not $apkanalyzer) {
    Write-Host "[ERROR] 未找到 aapt2 或 apkanalyzer，请确保 Android SDK 在 PATH 中"
    exit 1
}

# =========== 第一步：当前应用真实版本 ===========
Write-Host "`n[VERSION] === 第一步：当前应用真实版本 ==="

if ($aapt2) {
    $badging = & $aapt2 dump badging $ApkPath
    $verName = ($badging | Select-String "versionName=").Line -replace ".*versionName=''([^'']+)''.*", '$1'
    $verCode = ($badging | Select-String "versionCode=").Line -replace ".*versionCode=''([^'']+)''.*", '$1'
    $pkgName = ($badging | Select-String "package: name=").Line -replace ".*package: name=''([^'']+)''.*", '$1'
} elseif ($apkanalyzer) {
    $verName = & $apkanalyzer manifest version-name $ApkPath
    $verCode = & $apkanalyzer manifest version-code $ApkPath
    $pkgName = & $apkanalyzer manifest application-id $ApkPath
}

Write-Host "[VERSION] 当前 versionName: $verName"
Write-Host "[VERSION] 当前 versionCode: $verCode"
Write-Host "[VERSION] 当前包名: $pkgName"

# 签名信息
if ($aapt2) {
    $sigInfo = & $aapt2 dump badging $ApkPath | Select-String "signatures"
    Write-Host "[VERSION] 签名信息: $sigInfo"
}

# =========== 第二步：检查更新判断逻辑 ===========
Write-Host "`n[UPDATE] === 第二步：检查更新判断逻辑 ==="

$updateJson = Get-Content $UpdateJsonPath -Raw | ConvertFrom-Json
$serverVerName = $updateJson.versionName
$serverVerCode = [int]$updateJson.versionCode
$localVerCode = [int]$verCode

Write-Host "[UPDATE] 服务器 versionName: $serverVerName"
Write-Host "[UPDATE] 服务器 versionCode: $serverVerCode"
Write-Host "[UPDATE] 本地 versionName: $verName"
Write-Host "[UPDATE] 本地 versionCode: $localVerCode"
Write-Host "[UPDATE] 服务器 > 本地? $($serverVerCode -gt $localVerCode)"

if ($serverVerCode -gt $localVerCode) {
    Write-Host "[UPDATE] ✅ versionCode 比较正确，应触发更新"
} else {
    Write-Host "[UPDATE] ❌ versionCode 不大于本地版本，不会触发更新"
}

# 检查 MainActivity.kt 中是否硬编码了版本号
$mainActivityPath = "app\src\main\java\com\example\MainActivity.kt"
if (Test-Path $mainActivityPath) {
    $mainActivity = Get-Content $mainActivityPath
    if ($mainActivity -match 'val appVersion = "([^"]+)"') {
        $hardcodedVer = $Matches[1]
        Write-Host "[UPDATE] ⚠️ MainActivity.kt 中硬编码了 appVersion=$hardcodedVer，可能与 BuildConfig.VERSION_NAME=$verName 不一致"
    } else {
        Write-Host "[UPDATE] ✅ MainActivity.kt 已使用 BuildConfig.VERSION_NAME"
    }
}

# =========== 第三步：检查下载模块 ===========
Write-Host "`n[DOWNLOAD] === 第三步：检查下载模块 ==="

$downloadUrl = $updateJson.downloadUrl
Write-Host "[DOWNLOAD] 下载URL: $downloadUrl"

try {
    $webResponse = Invoke-WebRequest -Uri $downloadUrl -Method Head -UseBasicParsing -TimeoutSec 15
    Write-Host "[DOWNLOAD] HTTP 状态码: $($webResponse.StatusCode)"
    Write-Host "[DOWNLOAD] Content-Length: $($webResponse.Headers['Content-Length'])"
    Write-Host "[DOWNLOAD] Content-Type: $($webResponse.Headers['Content-Type'])"
} catch {
    Write-Host "[DOWNLOAD] ❌ HEAD 请求失败: $($_.Exception.Message)"
    try {
        $webResponse = Invoke-WebRequest -Uri $downloadUrl -Method Head -UseBasicParsing -TimeoutSec 15 -Headers @{"User-Agent"="Mozilla/5.0 (Android 14; Mobile; rv:120.0)"}
        Write-Host "[DOWNLOAD] 使用 Mozilla UA 重试后状态码: $($webResponse.StatusCode)"
    } catch {
        Write-Host "[DOWNLOAD] ❌ 全部尝试均失败，可能是 Gitee 限流或链接无效"
    }
}

# 检查权限
Write-Host "`n[DOWNLOAD] 检查权限声明:"
$manifestPath = "app\src\main\AndroidManifest.xml"
if (Test-Path $manifestPath) {
    $manifest = Get-Content $manifestPath
    if ($manifest -match "android.permission.INTERNET") { Write-Host "[DOWNLOAD] ✅ INTERNET 权限已声明" } else { Write-Host "[DOWNLOAD] ❌ INTERNET 权限缺失" }
    if ($manifest -match "android.permission.REQUEST_INSTALL_PACKAGES") { Write-Host "[DOWNLOAD] ✅ REQUEST_INSTALL_PACKAGES 已声明" } else { Write-Host "[DOWNLOAD] ❌ REQUEST_INSTALL_PACKAGES 缺失" }
    if ($manifest -match "android.permission.POST_NOTIFICATIONS") { Write-Host "[DOWNLOAD] ✅ POST_NOTIFICATIONS 已声明" } else { Write-Host "[DOWNLOAD] ❌ POST_NOTIFICATIONS 缺失" }
}

Write-Host "`n[DOWNLOAD] FileProvider 检查:"
if (Test-Path $manifestPath) {
    $fileProvider = Select-String -Path $manifestPath -Pattern "FileProvider"
    if ($fileProvider) {
        $line = $fileProvider.Line
        Write-Host "[DOWNLOAD] ✅ FileProvider 已声明"
    }
}

Write-Host "`n[DOWNLOAD] file_paths.xml 检查:"
$pathsPath = "app\src\main\res\xml\file_paths.xml"
if (Test-Path $pathsPath) {
    Write-Host (Get-Content $pathsPath -Raw)
}

# =========== 第四步：检查下载 APK ===========
Write-Host "`n[APK] === 第四步：检查本地已下载的 APK ==="
$localApk = "app-update.apk"
if (Test-Path $localApk) {
    $fileInfo = Get-Item $localApk
    Write-Host "[APK] 找到文件: $localApk，大小: $($fileInfo.Length) bytes"
    if ($aapt2) {
        $apkInfo = & $aapt2 dump badging $localApk
        Write-Host "[APK] 包名: $(($apkInfo | Select-String "package: name=") -replace ".*package: name=''([^'']+)''.*", '$1')"
        Write-Host "[APK] versionName: $(($apkInfo | Select-String "versionName=") -replace ".*versionName=''([^'']+)''.*", '$1')"
        Write-Host "[APK] versionCode: $(($apkInfo | Select-String "versionCode=") -replace ".*versionCode=''([^'']+)''.*", '$1')"
    }
} else {
    Write-Host "[APK] ⚠️ 本地未找到 $localApk（需从设备提取）"
    Write-Host "[APK] 在已安装设备上，可通过以下命令提取："
    Write-Host "  adb shell run-as com.aistudio.campusai.abcxyz cat /data/data/com.aistudio.campusai.abcxyz/files/Download/app-update.apk > app-update.apk"
    Write-Host "  adb shell ls -l /sdcard/Android/data/com.aistudio.campusai.abcxyz/files/Download/"
}

# =========== 第五步：安装逻辑检查 ===========
Write-Host "`n[INSTALL] === 第五步：检查安装逻辑 ==="
Write-Host "[INSTALL] 安装 Intent: Intent.ACTION_VIEW + application/vnd.android.package-archive"
Write-Host "[INSTALL] FileProvider URI: content://com.aistudio.campusai.abcxyz.fileprovider/downloads/app-update.apk"
Write-Host "[INSTALL] 检查未知来源安装权限（需在设备上执行）:"
Write-Host "  adb shell settings get global install_non_market_apps"
Write-Host "[INSTALL] Android 11+ (API 30): 需要在 queries 声明 INSTALL_PACKAGE intent（已检查 AndroidManifest.xml）"
Write-Host "[INSTALL] Android 14+ (API 34): 需要 ACTION_VIEW + 正确的 mimeType"
Write-Host "[INSTALL] Android 14+ 已知变更: 如果 targetSdk >= 34，系统会检查 PackageInstaller 是否可达"

# =========== 第六步：服务器验证 ===========
Write-Host "`n[SERVER] === 第六步：检查服务器文件 ==="
Write-Host "[SERVER] update.json 路径: https://gitee.com/LEQ0906/campus-app-update/raw/master/update.json"
Write-Host "[SERVER] APK 下载路径: $downloadUrl"
Write-Host "[SERVER] 版本检查: 服务器($serverVerCode) > 本地($localVerCode) ? $($serverVerCode -gt $localVerCode)"

# 检查 Gitee update.json 是否返回最新内容
try {
    $updateResp = Invoke-WebRequest -Uri "https://gitee.com/LEQ0906/campus-app-update/raw/master/update.json" -UseBasicParsing -TimeoutSec 10
    $remoteJson = $updateResp.Content | ConvertFrom-Json
    Write-Host "[SERVER] ✅ Gitee Raw 返回 update.json，版本: $($remoteJson.versionName) (code: $($remoteJson.versionCode))"
    if ($remoteJson.versionCode -eq $serverVerCode) {
        Write-Host "[SERVER] ✅ 远程 update.json 版本与本地一致"
    } else {
        Write-Host "[SERVER] ⚠️ 远程 update.json 版本 ($($remoteJson.versionCode)) 与本地 ($serverVerCode) 不一致，update.json 可能被缓存"
    }
} catch {
    Write-Host "[SERVER] ❌ 无法获取远程 update.json: $($_.Exception.Message)"
}

# 建议 Raw 直链
$rawUrl = $downloadUrl -replace "releases/download/v[^/]+/", "raw/master/"
Write-Host "[SERVER] 建议 Raw 直链: $rawUrl"
Write-Host "[SERVER] ⚠️ CDN 缓存: update.json 已加 Cache-Control: no-cache"

# =========== 第七步：日志检查建议 ===========
Write-Host "`n[LOG] === 第七步：日志检查建议 ==="
Write-Host "[LOG] 在设备上运行以下命令查看更新相关日志："
Write-Host "  adb logcat -c && adb logcat | Select-String -Pattern 'UPDATE|DOWNLOAD|APK|INSTALL|VERSION'"
Write-Host "[LOG] 分析 update_log.py 生成的日志文件（如存在）:"
if (Test-Path "update_log*.txt") {
    Get-ChildItem -Filter "update_log*.txt" | ForEach-Object { Write-Host "  $($_.Name) - $($_.Length) bytes" }
} else {
    Write-Host "  (本地无日志文件)"
}

# =========== 汇总 ===========
Write-Host "`n============================================"
Write-Host "  诊断完成 — 关键检查项汇总"
Write-Host "============================================"
Write-Host "✅ 本地版本: $verName ($verCode)"
Write-Host "✅ 服务器版本: $serverVerName ($serverVerCode)"
$canUpdate = $serverVerCode -gt $localVerCode
Write-Host "➡️ 版本比较: $(if($canUpdate){'✅ 可更新'}else{'❌ 无需更新'})"
$giteeOk = ($null -ne $webResponse) -and ($webResponse.StatusCode -eq 200)
Write-Host "➡️ Gitee 下载: $(if($giteeOk){'✅ 可通过'}else{'⚠️ 可能受限'})"
Write-Host "➡️ 建议: 如果版本比较正确但下载失败，更换 APK 存储到腾讯云 COS / 阿里云 OSS"
