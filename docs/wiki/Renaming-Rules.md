# 重命名规则（RenameMode）详解

实现位置：`app/src/main/java/com/maomaochongapp/renamer/`

本项目的重命名为两步：
1. 生成“计划”：`RenamePlanner.plan(originalNames, mode)`
2. 标注冲突：`RenamePlanner` 内部 `markConflicts()`

输出为 `List<RenamePreviewItem(oldName, newName, conflict)>`。

## 规则一：点读贴 REC（`RenameMode.RecSticker`）

适用：点读贴/录音贴，常见命名 `REC1205.mp3`。

参数：
- `startCode`：起始编码（例如 1201）
- `prefix`：默认 `REC`
- `digits`：编码位数（默认 4；UI 限制 1~8）
- `sort`：排序方式（按名称升序/降序，大小写不敏感）

规则说明：
- 按排序后的顺序依次生成：`prefix + code(padStart(digits,'0')) + 扩展名`
- 扩展名来自原文件名最后一个 `.` 之后（例如 `.mp3`），无扩展名则为空

示例：
- `a.mp3, b.mp3, c.mp3` + `startCode=1201` → `REC1201.mp3, REC1202.mp3, REC1203.mp3`

## 规则二：顺序编号（`RenameMode.IndexPrefix`）

适用：想按播放顺序编号，例如 `0001.mp3` 或 `0001_原名.mp3`。

参数：
- `startIndex`：起始序号（默认 1；UI 限制 ≥0）
- `width`：序号位数（默认 4；UI 限制 1~8）
- `separator`：保留原名时的分隔符（默认 `_`）
- `keepOriginal`：是否保留原文件名主体
- `sort`：排序方式（按名称升序/降序，大小写不敏感）

规则说明：
- `keepOriginal=false`：新名 = `indexPrefix + 扩展名`
- `keepOriginal=true`：新名 = `indexPrefix + separator + 原文件名主体 + 扩展名`

示例：
- `hello.mp3` → `0001.mp3`（不保留原名）
- `hello.mp3` → `0001_hello.mp3`（保留原名）
- `README`（无扩展名） → `0001_README`

## 规则三：正则替换（`RenameMode.RegexReplace`）

适用：批量把空格换成下划线、删除某些前缀等。

参数：
- `pattern`：Kotlin `Regex` 字符串（例如 `" "`、`"^\\d+_"`）
- `replacement`：替换字符串（例如 `"_"`、`""`）

规则说明：
- 只对“文件名主体”（不含扩展名）做替换，然后拼回原扩展名
- `pattern` 无法编译时：所有项标记 `conflict="正则表达式无效"`，且 `newName=oldName`
- 替换过程若抛异常：保持主体不变（因此可能触发“名称未变化”冲突）

## 排序（`Sort`）

实现：`String.CASE_INSENSITIVE_ORDER`

- `ByNameAsc`：按名称升序（不区分大小写）
- `ByNameDesc`：按名称降序（升序后反转）

注意：正则替换模式不排序（按原输入顺序）。

## 冲突判定（`RenamePlanner.markConflicts`）

按以下优先级标注（命中则不再继续判断）：
1. 已存在的 `item.conflict`（例如“正则表达式无效”）
2. `newName == oldName` → `名称未变化`
3. `newName.isBlank()` → `新名称为空`
4. 多个文件生成相同 `newName` → `新名称重复`
5. 否则为无冲突（可执行）

