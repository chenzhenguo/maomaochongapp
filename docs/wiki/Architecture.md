# 架构与代码结构

本项目是一个**单 Activity + 单 ViewModel + Tab化 Compose UI** 的实现，核心目标是提供完整的毛毛虫点读笔资源管理功能。

## 模块划分

代码主目录：`app/src/main/java/com/maomaochongapp/`

- `MainActivity.kt`：入口 Activity，仅负责注入 `MainViewModel` 并展示 `MainTabsScreen`
- `MainViewModel.kt`：业务编排层（SAF 授权、读取文件列表、生成预览、执行改名、执行复制、执行下载）
- `renamer/`
  - `RenameMode.kt`：重命名规则模型（3 种 mode + 2 种排序）
  - `RenamePlanner.kt`：纯函数式"改名计划生成器"（输入旧名列表 + mode，输出预览列表 + 冲突标记）
- `sequence/`
  - `SequenceAllocator.kt`：序号分配器（序号提取、分配、避免重复）
- `export/`
  - `SafExporter.kt`：基于 `DocumentFile` 的复制导出工具（创建目录、复制文件、覆盖策略）
- `download/`
  - `DownloadModels.kt`：下载相关数据模型
- `ui/`
  - `MainTabsScreen.kt`：Tab化主界面
  - `tabs/`：各Tab页面组件
  - `components/`：可复用UI组件

## 数据流（UI → ViewModel → 纯逻辑/IO）

- UI 层 `MainTabsScreen(viewModel)` 订阅 `viewModel.state: StateFlow<MainUiState>`
- 用户操作（选择目录/点击按钮/改参数）调用 ViewModel 的方法：
  - `onFolderPicked(uri)`：记录并持久化 SAF 权限，读取目录下文件列表
  - `buildPreview()`：用 `RenamePlanner.plan()` 生成改名预览（不落地）
  - `applyRename()`：逐个 `DocumentFile.renameTo()` 执行改名（跳过冲突项）
  - `onExportRootPicked(uri)`：记录目标目录 URI
  - `buildCopyPreview()`：生成复制预览（基于序号分配）
  - `applyCopy()`：执行复制到目标子目录
  - `buildDownloadPreview()`：生成下载预览（基于序号分配）
  - `applyDownload()`：执行下载到目标子目录
  - `scanTargetAndJumpToNextIndex()`：扫描目标目录并跳转到下一个可用序号

耗时操作统一在 `viewModelScope` 中切到 `Dispatchers.IO`，并通过 `MainUiState.isBusy` 控制 UI 按钮可用性。

## UI 状态（`MainUiState` 关键字段）

- 源目录：
  - `folderUri` / `folderName`
  - `files: List<DocumentFile>`（仅文件，不包含子目录）
  - `selectedFileNames: Set<String>`（文件页多选状态）
- 目标目录：
  - `exportRootUri` / `exportRootName`
  - `targetSubdir`：目标子目录（MP3/DIYAUDIO/REC等）
  - `targetFiles: List<DocumentFile>`（目标子目录文件列表）
- 序号配置：
  - `sequenceNext`：当前序号
  - `sequenceWidth`：序号位数
  - `sequencePrefix`：序号前缀
  - `avoidDuplicateSequence`：是否避免重复
- 重命名：
  - `mode: RenameMode`
  - `preview: List<RenamePreviewItem>`（`oldName/newName/conflict`）
- 复制：
  - `copyPreview: List<ExportPreviewItem>`
  - `copyOverrideStartIndex`：临时起始序号
  - `plannedCopyNextAfter`：计划的下一个序号
- 下载：
  - `downloadUrlsText`：URL输入文本
  - `downloadPreview: List<DownloadPreviewItem>`
  - `plannedDownloadNextAfter`：计划的下一个序号
- 其他：
  - `exportOverwrite`：是否覆盖同名文件
  - `debugLogs`：调试日志
  - `lastMessage`：最近消息
  - `isBusy`：是否正在执行操作

## 冲突（conflict）约定

`RenamePlanner` 会在生成计划后统一标注冲突：
- `正则表达式无效`
- `名称未变化`
- `新名称为空`
- `新名称重复`

复制/下载预览会在此基础上继续叠加：
- `无法创建目标目录`
- `文件名包含非法字符`（包含 `/` 或 `\\`）
- `目标已存在`（仅当未勾选覆盖）

## 序号管理

v2版本的核心特性是序号管理：
- **按子目录维护**：不同子目录分别维护序号（通过SharedPreferences键名区分）
- **自动递增**：操作成功后自动更新序号
- **避免重复**：扫描目标目录现有文件，跳过已使用的序号
- **持久化**：序号配置保存在SharedPreferences中