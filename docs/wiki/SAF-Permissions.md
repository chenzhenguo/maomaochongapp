# SAF 授权与目录选择（OpenDocumentTree）

本项目使用 Android 的 Storage Access Framework（SAF）访问用户选定目录。

代码入口：
- `MainScreen.kt`：`ActivityResultContracts.OpenDocumentTree()`
- `MainViewModel.onFolderPicked/onExportRootPicked`：`takePersistableUriPermission`

## 为什么要用 SAF

- 避免直接申请存储权限（尤其是 Android 10+ 分区存储）
- 用户明确选择目录，App 只对该目录具备读写能力（取决于 Provider）
- 同时支持手机存储、SD 卡、OTG/U盘（前提是系统与 Provider 支持写入）

## 持久化授权（Persistable URI Permission）

`MainViewModel` 会尝试调用：

`contentResolver.takePersistableUriPermission(treeUri, READ|WRITE)`

注意：
- 部分 Provider 不支持持久化授权，会抛 `SecurityException`
- 项目当前策略：捕获后忽略异常，继续“当前会话”使用该 URI

## 常见问题与建议

- 选择目录后看不到文件：
  - 本项目仅处理“目录下的文件”，会过滤掉子目录（`filter { it.isFile }`）
  - 如资源在子目录，请在系统文件选择器中直接进入子目录再选择

- 无法创建目标目录/无法写入：
  - 换一个目标目录（例如选择外置存储的更上层目录）
  - 确认目标存储介质是可写的（部分 OTG/U盘 在某些机型上被限制写入）

- 下次进入需要重新授权：
  - 属于 Provider 行为差异（或系统清理了权限），目前只能重新选择目录

