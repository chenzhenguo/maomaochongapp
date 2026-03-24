# v2 设计文档：Tab 化文件管理 + 序号管理 + 复制/下载

> 日期：2026-03-19  
> 目标：将现有单屏“改名/导出”升级为 4 Tab：文件、复制、下载、设置；新增序号（编号）管理与面向点读笔磁盘目录的默认目标子目录。

## 1. 设计原则

- 以“点读笔磁盘根目录 + 子目录”为核心（默认子目录列表）
- 序号是第一公民：可见、可控、可持久化、可避免重复
- 预览优先：所有批量操作先预览、再执行；冲突项默认跳过
- 复用现有能力：`RenamePlanner`、`SafExporter`、`MainViewModel` 的 IO 组织方式

## 2. 页面与交互（信息架构）

### 2.1 Tab 划分

1) 文件（File Manager）
- 源文件列表（来自“源读取文件夹”）
- 目标文件列表（来自“目标根 + 当前子目录”）
- 多选、批量命名预览、批量复制/移动、（可选）批量删除

2) 复制（Copy）
- 以“从源选择 → 按序号命名 → 复制到目标子目录”为主流程
- 可临时调整序号起点、覆盖策略、目标子目录

3) 下载（Download）
- 输入 URL 列表 → 预览（目标名/目标路径）→ 下载执行
- 与复制共享同一套“序号分配/去重/覆盖”策略

4) 设置（Settings）
- 配置源 URI、目标根 URI（SAF）
- 选择默认目标子目录（含自定义）
- 序号配置（按作用域持久化）
- 命名模板配置（建议先做最小可行：数字+可选前缀/位数）

### 2.2 默认目标子目录

常用目录常量（用于下拉与默认值）：
- `BOOK`
- `DICT`
- `DIYAUDIO`
- `DIYREC`
- `MP3`
- `DIYREC2`
- `REC`

实现：`enum class PenDir(val dirName: String)` 或仅常量列表。

## 3. 数据模型与持久化

### 3.1 Settings（DataStore）

新增 `SettingsRepository`（建议使用 `androidx.datastore:datastore-preferences`）持久化：
- `sourceTreeUri: String?`
- `destRootTreeUri: String?`
- `defaultTargetSubdir: String`（默认 `MP3`）
- `overwrite: Boolean`（默认 false）

序号相关（建议按子目录分别维护，避免不同目录互相干扰）：
- `sequence.current.<subdir>`：Int（例如 `sequence.current.MP3=1`）
- `sequence.width.<subdir>`：Int（默认 4，可选）
- `sequence.prefix.<subdir>`：String（默认空；用于 REC 类目录可设为 `REC`，TBD）

理由：
- MP3 与 DIYAUDIO/DIYREC 的命名规则通常不同，按子目录维护更贴近用户预期

### 3.2 UI State（ViewModel）

建议拆分为 2 层：

- 全局 `AppState`（来自 DataStore 的设置流 + 当前选中的 subdir）
- 局部 `ScreenState`（每个 Tab 的临时输入，如下载 URL 文本、当前多选集合、临时序号等）

但为保持改动可控，v2 初期可仍使用单个 `MainViewModel`，新增字段：
- `sourceUri/sourceName/sourceFiles`
- `destRootUri/destRootName`
- `targetSubdir`
- `destFiles`（当前子目录文件）
- `sequenceCurrent`（从 Settings 读取；切换子目录时更新）
- `tempSequenceOverride`（复制/下载页临时调整）
- `downloadUrlsText` / `downloadPreview`

## 4. 核心算法设计

### 4.1 文件列表读取

复用现有 `DocumentFile.fromTreeUri` + `listFiles().filter { it.isFile }`。

文件页需要两份列表：
- 源：`sourceFolder.listFiles()...`
- 目标：`destRoot/targetSubdir` 目录存在时读取（不存在则尝试创建或提示）

### 4.2 序号提取（Avoid Duplicates）

需要一个“从文件名提取序号”的函数，基于命名模板：

**最小可行模板（推荐 v2 MVP）**
- 模板：`{prefix}{num}.{ext}`
- 例：`0001.mp3`（prefix=""，width=4）
- 例：`REC1201.mp3`（prefix="REC"，width=4）

提取规则：
- 若 prefix 非空：要求文件名以 prefix 开头
- 从 prefix 后连续数字段提取为 num
- 允许后面跟任意内容（例如扩展名、下划线原名），但 v2 MVP 可先限制为“紧接扩展名”

产出：
- `used: Set<Int>`（已使用序号集合）

### 4.3 序号分配（Allocate）

输入：
- `start: Int`（当前序号或用户临时覆盖）
- `count: Int`（需要分配数量）
- `used: Set<Int>`（目标目录已存在的序号）

输出：
- `assigned: List<Int>`（长度=count，严格递增、跳过 used）
- `nextCurrent: Int`（最后一个 assigned + 1）

规则：
- 分配时不断递增，若 num 在 used 中则跳过
- 若用户选择“防止重复”开关关闭（不建议默认关闭），则不扫描 used

### 4.4 复制/下载命名（Generate Names）

根据模板生成目标名：
- `newName = prefix + num.padStart(width,'0') + ext`

ext 来源：
- 复制：取源文件扩展名（沿用现有 `RenamePlanner.splitName` 逻辑）
- 下载：优先从 URL path 推断；其次从 Content-Type 推断；失败则 `.bin`（TBD）

冲突规则（预览阶段）：
- 目标已存在且 `overwrite=false` → conflict
- 名称含 `/` 或 `\\` → conflict
- 同批次新名称重复（通常不会发生，除非模板/分配错误）→ conflict

### 4.5 执行（SafExporter 扩展）

当前 `SafExporter.export()` 是“源目录内 byName 查 DocumentFile，再 copyFile 到 destDir”，适合复制模式。

v2 复制模式：
- 计划结构从 `(oldName -> newName)` 扩展为直接传 `DocumentFile`（更稳，避免重名导致 byName 错配）
- 建议新增：
  - `copyFiles(contentResolver, sources: List<DocumentFile>, destDir, names: List<String>, overwrite)`

下载模式：
- 新增 `Downloader`（OkHttp 或 URLConnection）：
  - 先下载到 app 私有缓存文件（或直接写 SAF OutputStream）
  - 写入目标 `DocumentFile`（SAF createFile + openOutputStream）

## 5. 代码结构建议（模块化）

新增包（建议）：
- `settings/SettingsRepository.kt`：DataStore 读写
- `sequence/SequenceManager.kt`：序号提取、分配、命名模板
- `download/Downloader.kt`：下载到 SAF
- `ui/tabs/*`：按 Tab 拆分 Compose 文件，降低 `MainScreen.kt` 单文件体积

保留并复用：
- `export/SafExporter.kt`
- `renamer/RenamePlanner.kt`（仍可用于“按名称规则改名”的高级模式；序号命名建议走 SequenceManager）

## 6. 失败处理与可取消

- 所有 IO 在 `Dispatchers.IO` 执行
- 复制/下载支持“取消”（通过保存 Job 或使用协程取消）
- 失败项记录原因，执行不中断；最后给汇总：ok/skipped/failed

## 7. 测试策略

优先补单测（纯逻辑）：
- `SequenceManager.extractUsedNumbers()`
- `SequenceManager.allocateNumbers()`
- `SequenceManager.buildName()`

其次是 `SafExporter` 的拆分逻辑（可通过接口抽象或把纯路径逻辑单测化）。

## 8. 风险与未决项（TBD）

1) 命名模板是否因目标子目录不同而不同（`REC` 前缀 vs 纯数字）  
2) 序号作用域：全局还是按子目录  
3) 下载扩展名策略与音频格式限制  
4) “批量关联”含义：是“文件与目标子目录/序号段的映射”，还是“绘本项目化管理”需要保留？

