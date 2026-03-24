# 开发者指南

## 技术栈

- Kotlin 1.9.24
- Jetpack Compose + Material3（Compose BOM 2024.06.00）
- Gradle 8.5.2（Kotlin DSL）
- minSdk 26 / targetSdk 35

依赖配置：`app/build.gradle.kts`

## 环境准备

建议：
- Android Studio（与 AGP 8.5.x 匹配）
- JDK 17
- Android SDK（platforms 35 + build-tools 35.0.0）

如果你在 Windows 环境缺 SDK，可用仓库脚本安装到默认目录：

`python tools/setup_android_sdk.py`

脚本会使用（可覆盖）：
- `ANDROID_SDK_ROOT`：Android SDK 安装目录

如遇到 Gradle Wrapper 的 `gradle-wrapper.jar` 损坏/缺失，可使用修复脚本：

`python tools/repair_gradle_wrapper_jar.py`

## 常用构建命令

在仓库根目录执行：

- Debug APK：`./gradlew :app:assembleDebug`
- 单元测试（RenamePlanner）：`./gradlew :app:testDebugUnitTest`
- Lint（如启用）：`./gradlew :app:lintDebug`

## 版本与签名说明

- `versionName = "0.1.0"`：`app/build.gradle.kts`
- `release` 构建开启 `minify` 与 `shrinkResources`
- 当前 `release` 使用 `debug` signingConfig（便于本地打包验证；正式发布前应改为独立 keystore）

## 代码约定（现状）

- 业务尽量集中在 `MainViewModel.kt`，UI 仅负责收集状态与触发事件
- 重命名规则与计划生成保持纯逻辑（`RenameMode`/`RenamePlanner`），便于单测覆盖
- SAF IO 集中在 `SafExporter`，减少 UI/ViewModel 中的细节分散
