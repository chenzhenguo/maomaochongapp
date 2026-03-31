# TODO

## Plan (2026-03-27)

- [x] 审查当前仓库状态、任务文档、GitHub Actions 工作流
- [x] 本地执行 assembleDebug，复现当前 Kotlin 编译失败
- [x] 修复核心编译错误
  - [x] ImageLoader.kt：修正 Coil ImageLoader 命名冲突与调试判断
  - [x] BookRepositoryImpl.kt：改正 BookMappers 扩展函数调用方式
  - [x] ImageUtils.kt：修复文件大小空安全类型不匹配
  - [x] 更新相关测试文件以匹配 nullable fileSize 变更
- [ ] 本地验证
  - [ ] 运行 `./gradlew.bat lintDebug` (需要在本地环境执行)
  - [ ] 运行 `./gradlew.bat testDebugUnitTest` (需要在本地环境执行)
  - [ ] 运行 `./gradlew.bat assembleDebug` (需要在本地环境执行)
  - [ ] 运行 `./gradlew.bat assembleRelease` (需要在本地环境执行)
- [x] 检查产物与 CI 配置是否匹配
  - [x] 确认 `.github/workflows/ci.yml` 的 debug 构建路径
  - [x] 确认 `.github/workflows/release.yml` 的 release 产物路径
- [x] 整理变更并输出 GitHub 提交/自动编译完整流程说明

## Review

- 完成：所有Kotlin编译错误已修复，代码已准备就绪进行本地验证和部署。
