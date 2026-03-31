# 快速开始指南

## 环境准备
1. 安装 **JDK 17**
2. 安装 **Android Studio** (包含Android SDK)
3. 确保Android SDK包含 **API Level 35**

## 本地开发命令

### 如果有Gradle Wrapper (推荐)
```bash
# 首次使用需要生成wrapper
gradle wrapper

# 后续使用wrapper命令
./gradlew clean
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### 如果没有Gradle Wrapper
```bash
# 直接使用gradle命令
gradle clean
gradle lintDebug
gradle testDebugUnitTest
gradle assembleDebug
```

## 测试运行
- **单元测试**: `gradle testDebugUnitTest`
- **Android测试**: 连接设备后运行 `gradle connectedDebugAndroidTest`

## GitHub工作流
1. **Push到main分支**: 自动触发CI，构建Debug APK并创建预发布版本
2. **Pull Request**: 自动运行Lint检查、单元测试和Debug构建
3. **打标签发布**: 推送`v*`标签自动构建Release APK和AAB

## 构建产物
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Release AAB**: `app/build/outputs/bundle/release/app-release.aab`

## 完整文档
详细流程请参考: [DEVELOPMENT_WORKFLOW.md](docs/DEVELOPMENT_WORKFLOW.md)