# Project Long-Term Memory

## 项目概览
- **项目名**：毛毛虫 App（maomaochongapp）
- **类型**：Android 绘本管理应用
- **技术栈**：Kotlin 1.9.24 + Jetpack Compose + Room + Coil 2.6.0，AGP 8.5.2，Java 17

## 关键架构约定
- `BookMappers.kt`：扩展函数为**文件级顶层函数**（不在 object 内），可直接 `import com.maomaochongapp.picturebook.data.mapper.toDomain`
- `AppImageLoader`（原 `ImageLoader`）：已重命名避免与 Coil 命名冲突，文件仍叫 `ImageLoader.kt`
- Repository 模式：`BookRepositoryImpl` 注入 `BookDao`
- ViewModel：`PictureBookViewModel` 继承 `AndroidViewModel`，使用 `MutableStateFlow`

## GitHub Actions CI/CD
- **ci.yml**：push/PR to main 触发 → lintDebug → testDebugUnitTest → assembleDebug → auto release（pre-release）
- **release.yml**：`v*` tag 触发 → assembleRelease + bundleRelease → GitHub Release
- SDK 路径：使用 ubuntu-latest 内置 `$ANDROID_HOME`（不用弃用的 setup-android@v3）
- 签名 Secrets（release 时设置）：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`
- 本地无 secrets 时自动降级为 debug 签名

## 编译验证（2026-03-27）
- Debug APK：BUILD SUCCESSFUL ✅
- Release APK：BUILD SUCCESSFUL ✅

## 约定
- 不在本地跑完整编译验证时，通过 CI 日志迭代；有修改时优先本地验证
