# CampusAI — 校园达人

基于 **柳比歇夫时间记录法** 的校园效率工具，集成 **Gemini AI** 时间分析 + **闲置交易** 功能。

## 功能概览

| 模块 | 说明 |
|------|------|
| 📊 仪表盘 | 今日专注时长统计、等级成长体系 |
| ⏱ 时间统计 | 按「学习/运动/娱乐」分类记录时间，遵循柳比歇夫方法 |
| 🤖 AI 分析 | 一键生成 Gemini AI 深度时间分析报告（中文） |
| 🔄 闲置交易 | 校园二手物品发布与浏览 |
| 👤 个人中心 | 用户资料与设置 |

## 技术栈

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 数据库 | Room (SQLite) |
| AI | DeepSeek API（OpenAI 兼容，Retrofit + Kotlin Serialization） |
| 构建 | Gradle Kotlin DSL + Version Catalog |
| 测试 | Roborazzi (截图) + Robolectric + JUnit |
| 密钥 | Secrets Gradle Plugin (`.env`) |

## 项目结构

```
app/
├── src/main/java/com/example/
│   ├── MainActivity.kt          # 主 Activity + 底部导航
│   ├── ai/
│   │   └── GeminiApiService.kt  # Gemini API 请求/响应封装
│   ├── data/
│   │   ├── AppDatabase.kt       # Room 数据库（TimeRecord, GoodItem）
│   │   └── AppRepository.kt     # 数据仓库
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── DashboardScreen.kt
│   │   │   ├── TimeTrackerScreen.kt
│   │   │   ├── AiAnalysisScreen.kt
│   │   │   ├── BarterScreen.kt
│   │   │   └── ProfileScreen.kt
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── viewmodel/
│       └── MainViewModel.kt     # 全局 ViewModel
├── src/test/                    # 单元测试 + Roborazzi 截图测试
└── src/androidTest/             # 仪器化测试
```

## 快速开始

### 前置要求

- Android Studio（最新稳定版）
- JDK 17+
- Android SDK 36
- DeepSeek API 密钥（从 [DeepSeek Platform](https://platform.deepseek.com/api_keys) 获取）

### 运行步骤

1. 克隆项目

   ```bash
   git clone https://github.com/liburce0412-alt/322-building-program.git
   cd 322-building-program
   ```

2. 在项目根目录创建 `.env` 文件，写入你的 API 密钥：

   ```env
   GEMINI_API_KEY=你的密钥
   ```

3. 用 Android Studio 打开项目目录，等待 Gradle 同步完成

4. 移除 `app/build.gradle.kts` 中 debug 签名配置相关行（仅用于首次本地调试）：

   注释掉或删除：
   ```kotlin
   // signingConfig = signingConfigs.getByName("debugConfig")
   ```

5. 连接设备或启动模拟器，点击 Run

> **注意**：`debug.keystore` 和 `debug.keystore.base64` 已通过 `.gitignore` 排除，不会提交到仓库。

## 构建与发布

### Debug APK

```bash
./gradlew assembleDebug
```

APK 生成路径：`app/build/outputs/apk/debug/app-debug.apk`

### Release APK

需要先配置签名信息（环境变量方式）：

```bash
export KEYSTORE_PATH=/path/to/your-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
./gradlew assembleRelease
```

### 安装到设备

```bash
./gradlew installDebug
```

## 测试

```bash
# 单元测试 + Robolectric
./gradlew test

# Roborazzi 截图测试
./gradlew verifyRoborazziDebug
```

## 许可证

Apache 2.0
