# 测试

当前测试集中在"重命名计划生成器"和"序号分配器"，因为它们是纯逻辑、可稳定单测的核心模块。

## 单元测试位置

- `app/src/test/java/com/maomaochongapp/renamer/RenamePlannerTest.kt`
- `app/src/test/java/com/maomaochongapp/sequence/SequenceAllocatorTest.kt`

覆盖点（示例）：
- REC 规则生成
- 顺序编号（保留/不保留原名）
- 正则替换（含无效正则）
- 冲突：新名称重复、名称未变化、新名称为空
- 边界：无扩展名、尾部点号文件名、大小写无关排序
- 序号提取：从文件名中提取序号（带前缀和不带前缀）
- 序号分配：避免重复、自动递增、起始序号处理

## 运行测试

在仓库根目录：

`./gradlew :app:testDebugUnitTest`

## 扩展建议

如果后续要增强可靠性，优先补齐以下测试维度：
- `SafExporter.buildDestRelativePath/ensureDirectory` 的单测（可用 fake DocumentFile 或拆出纯逻辑）
- `RegexReplace` 对复杂 replacement（例如 `$1`）的行为约定与测试
- `RenamePlanner.splitName` 对更多文件名形态（多点、隐藏文件等）的预期
- **v2新增功能测试**：
  - 复制预览生成逻辑
  - 下载预览生成逻辑
  - 序号按子目录分别维护的持久化
  - 目标目录扫描和序号跳转功能
  - URL到文件名的扩展名推断逻辑