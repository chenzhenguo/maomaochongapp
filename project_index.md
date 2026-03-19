# 毛毛虫资源管家 - 项目文件索引

> 最后更新：2026-03-19

## 项目概览

| 属性 | 值 |
|------|-----|
| 项目名称 | maomaochongapp（毛毛虫资源管家） |
| 包名 | `com.maomaochongapp` |
| 技术栈 | Kotlin + Jetpack Compose + Material3 |
| 构建工具 | Gradle 8.5.2 (Kotlin DSL) |
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 35 |
| Compose BOM | 2024.06.00 |
| Kotlin 版本 | 1.9.24 |
| JVM Target | 17 |
| Foundation | androidx.compose.foundation（显式依赖）|

## 目录结构

```
maomaochongapp/
├── app/                              # 应用主模块
│   ├── build.gradle.kts              # 模块级构建配置（依赖、编译选项、签名）
│   ├── proguard-rules.pro            # ProGuard 混淆规则（空）
│   └── src/main/
│       ├── AndroidManifest.xml       # 应用清单（单 Activity）
│       ├── java/com/maomaochongapp/
│       │   ├── MainActivity.kt       # 应用入口 Activity
│       │   ├── MainViewModel.kt      # 核心业务逻辑 ViewModel + UI 状态定义
│       │   ├── renamer/
│       │   │   ├── RenameMode.kt     # 重命名规则密封类定义（RecSticker/IndexPrefix/RegexReplace）
│       │   │   └── RenamePlanner.kt  # 重命名计划生成器（排序、命名、冲突检测）
│       │   ├── export/
│       │   │   └── SafExporter.kt    # SAF 文件导出器（目录创建、文件复制/移动）
│       │   └── ui/
│       │       └── MainScreen.kt     # Compose UI 主界面及所有子组件
│       ├── res/
│       │   └── values/
│       │       ├── strings.xml       # 字符串资源（app_name = 毛毛虫资源管家）
│       │       └── themes.xml        # 主题配置（Material3 DayNight NoActionBar）
│       └── test/
│           └── java/com/maomaochongapp/
│               └── renamer/
│                   └── RenamePlannerTest.kt  # RenamePlanner 单元测试（14 个测试用例）
├── docs/
│   └── PRD.md                        # 产品需求文档
├── tools/
│   ├── repair_gradle_wrapper_jar.py  # Gradle wrapper 修复脚本
│   └── setup_android_sdk.py          # Android SDK 安装脚本
├── build.gradle.kts                  # 根级构建配置
├── settings.gradle.kts               # 项目设置（单模块 :app）
├── gradle.properties                 # Gradle 属性
└── .gitignore                        # Git 忽略规则
```

## 核心模块说明

### 1. 入口层 (`MainActivity.kt`)
- 唯一的 `ComponentActivity`，通过 `viewModels()` 持有 `MainViewModel`
- 使用 `setContent` 加载 Compose UI，外层 `MaterialTheme` + `Surface`

### 2. 状态管理层 (`MainViewModel.kt`)
- 继承 `AndroidViewModel`，使用 `MutableStateFlow<MainUiState>` 管理全局 UI 状态
- 核心功能：文件夹选择、重命名预览/执行、导出预览/执行
- 通过 `viewModelScope` + `Dispatchers.IO` 处理耗时操作

### 3. 重命名引擎 (`renamer/`)
- `RenameMode` — 密封接口，定义三种重命名规则
- `RenamePlanner` — 纯函数式计划生成器，含冲突检测逻辑

### 4. 导出引擎 (`export/`)
- `SafExporter` — 基于 SAF（Storage Access Framework）的文件操作
- 支持目录递归创建、文件复制/移动、覆盖策略

### 5. UI 层 (`ui/MainScreen.kt`)
- 单屏应用，所有 UI 组件在一个文件中
- 包含：`MainScreen`、`ModeEditor`、`RecStickerEditor`、`IndexPrefixEditor`、`RegexEditor`、`SortEditor`、`ExportEditor`、`PreviewPane`

### 6. 单元测试层 (`src/test/`)
- `RenamePlannerTest` — 覆盖 `RenamePlanner` 关键分支的 14 个 JUnit4 测试用例
- 测试场景：REC 编码生成、名称升/降序排序、顺序编号（保留/不保留原名）、正则替换、无效正则、无效替换分组、重名冲突检测、名称未变化检测、新名称为空、无扩展名文件处理、尾部点号文件名、自定义起始序号

---

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-03-19 | 修复 `MainScreen.kt` 中 `KeyboardOptions` 错误 import 路径（`ui.text.input` → `foundation.text`） |
| 2026-03-19 | `app/build.gradle.kts` 补充 `androidx.compose.foundation:foundation` 显式依赖 |
| 2026-03-19 | 删除根目录无用临时文件 `delete_test.tmp`、`delete_small.tmp` |
| 2026-03-19 | 补录 `src/test/` 单元测试目录结构及模块说明 |
| 2026-03-19 | `RenamePlannerTest` 新增 4 个边界测试，覆盖空名称、无效替换分组、尾部点号文件名、大小写无关排序 |

