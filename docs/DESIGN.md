# 毛毛虫资源管家（App）设计文档

更新时间：2026-03-19

## 1. 总体架构

- UI：Jetpack Compose（Material3）
- 状态：`MainViewModel` + `StateFlow<MainUiState>`
- 文件访问：SAF（`DocumentFile` + `ContentResolver`）
- 写入能力：
  - 复制：`SafExporter`（流式复制到目标目录）
  - 下载：`HttpURLConnection` 拉取流，写入 SAF 目标文件
- 设置持久化：SharedPreferences（保存序号、位数、目标子目录、以及上次目录 URI 字符串；在权限有效时恢复）

## 2. 关键数据结构

### 2.1 UI 状态（简化）

- 源目录
  - `folderUri`、`folderName`、`files`
  - `selectedFileNames`
- 目标目录
  - `exportRootUri`、`exportRootName`
  - `targetSubdir`
  - `targetFiles`（浏览目标子目录时加载）
- 序号配置
  - `sequenceNext`（下一个可用序号）
  - `sequenceWidth`（零填充位数）
  - `avoidDuplicate`（避免与目标目录现有序号冲突）
- 预览
  - `renamePreview`（源内改名）
  - `copyPreview`（复制模式）
  - `downloadPreview`（下载模式）
- 其他
  - `overwriteExisting`
  - `isBusy`、`lastMessage`、`debugLogs`

### 2.2 预览模型

- Copy 复用 `ExportPreviewItem`：
  - `oldName` / `newName` / `destRelativePath` / `conflict`
- Download 新增 `DownloadPreviewItem`：
  - `url` / `destName` / `destRelativePath` / `conflict`

## 3. 序号分配与防重逻辑

### 3.1 扫描目标目录占用序号

从目标目录（`exportRootUri` + `targetSubdir`）列出文件名，解析“前导数字”作为已占用序号：

- 规则：从文件名开头提取连续数字（至少 1 位），转 Int
- 例：`0001.mp3` → 1，`12.wav` → 12，`REC1201.mp3` → 不解析（非数字开头）

### 3.2 分配算法

输入：

- `start`：`sequenceNext`（或本次覆盖起始序号）
- `count`：本次需要分配的数量
- `used`：已占用序号集合（仅在 avoidDuplicate=true 时参与）

输出：

- `allocated`：长度为 count 的序号列表，遇到冲突则顺延寻找下一个未占用序号
- `nextAfter`：`allocated.last() + 1`（用于操作完成后更新 `sequenceNext`）

### 3.3 序号更新策略

复制/下载执行成功（无论是否有个别失败）后：

- `sequenceNext = max(sequenceNext, plannedNextAfter)`
- 允许出现“跳号”（例如某个序号写入失败），以保证后续不会产生重复

## 4. SAF 路径与目录创建

目标目录 = `exportRootUri` 对应的 `DocumentFile` 根目录。

目标子目录（如 `MP3`）通过 `SafExporter.ensureDirectory(root, listOf("MP3"))` 创建/获取。

复制写入：

- `createFile(mime, name)` 创建目标文件
- `contentResolver.openInputStream(source.uri)` → `openOutputStream(dest.uri)` 流式拷贝

下载写入：

- `HttpURLConnection(url).inputStream` → `openOutputStream(dest.uri)` 流式拷贝
- MIME：依据扩展名猜测；无扩展名使用 `application/octet-stream`

## 5. UI 结构（Tab）

`MainScreen` 内部：

- `Scaffold`
  - `TabRow`（设置/文件/复制/下载）
  - Tab Content：
    - 设置页：目录选择、目标子目录、序号、覆盖、调试
    - 文件页：源/目标浏览切换、多选、批量改名、打开文件
    - 复制页：基于已选源文件生成预览并复制
    - 下载页：URL 列表生成预览并下载

## 6. 权限与恢复

目录选择使用 `takePersistableUriPermission` 尝试持久化授权。

App 启动时：

- 从 SharedPreferences 读取上次保存的 URI 字符串
- 若在 `contentResolver.persistedUriPermissions` 中仍存在该 URI，则恢复到 state 并自动加载文件列表

## 7. 错误处理与可观测性

- 所有 IO 操作在 `Dispatchers.IO` 中执行
- UI 上展示 `lastMessage`
- `debugLogs` 记录关键步骤与异常信息，并支持导出为文本

