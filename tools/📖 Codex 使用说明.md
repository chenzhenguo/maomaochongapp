# Codex 命令执行工具 - 使用说明

## 📁 文件位置

**项目目录**: `D:\ideaworkspace\maomaochongapp`

**工具位置**: `tools/` 目录

| 文件 | 用途 |
|------|------|
| `codex.bat` | Windows 批处理版本 |
| `codex.py` | Python 版本 |
| `📖 Codex 使用说明.md` | 本文件 |

---

## 🚀 快速开始

### 方法 1: 使用批处理文件

```cmd
cd D:\ideaworkspace\maomaochongapp
tools\codex.bat help
```

### 方法 2: 使用 Python 脚本

```bash
cd D:\ideaworkspace\maomaochongapp
python tools/codex.py help
```

### 方法 3: 直接运行 codex

```bash
cd D:\ideaworkspace\maomaochongapp
codex [命令]
```

---

## 📋 可用命令

### 计划模式

```bash
# 创建任务计划
python tools/codex.py plan 添加下载功能

# 进入 codex 计划模式
codex plan
```

### 构建项目

```bash
# 构建
python tools/codex.py build

# 或直接使用 gradle
gradlew build
```

### 运行测试

```bash
# 测试
python tools/codex.py test

# 或直接使用 gradle
gradlew test
```

### 清理项目

```bash
# 清理
python tools/codex.py clean

# 或直接使用 gradle
gradlew clean
```

### 执行 Codex 命令

```bash
# 直接运行 codex 命令
codex [你的命令]

# 或使用 npx
npx @anthropic-ai/codex [你的命令]
```

---

## 📝 任务管理

### 任务文件位置

**目录**: `D:\ideaworkspace\maomaochongapp\tasks\`

| 文件 | 用途 |
|------|------|
| `todo.md` | 任务清单和计划 |
| `lessons.md` | 经验教训记录 |

### 创建任务计划

运行以下命令会自动创建任务计划：

```bash
python tools/codex.py plan 添加批量下载功能
```

生成的计划文件：
```markdown
# 任务：添加批量下载功能

**创建时间**: 2026-03-25
**状态**: 🔄 计划中

---

## 目标

{{填写任务目标}}

---

## 子任务清单

- [ ] 1. {{子任务 1}}
- [ ] 2. {{子任务 2}}
- [ ] 3. {{子任务 3}}

---

## 执行记录

{{执行过程中填写}}

---

## 完成总结

{{完成后填写}}
```

---

## 🔧 安装 Codex CLI

### 方法 1: 使用 npm

```bash
npm install -g @anthropic-ai/codex
```

### 方法 2: 使用 npx（无需安装）

```bash
npx @anthropic-ai/codex [命令]
```

### 验证安装

```bash
codex --version
```

---

## 📊 工作流程

### 标准流程

```
1. 创建任务计划
   ↓
2. 进入 Codex 计划模式
   ↓
3. Codex 生成详细计划
   ↓
4. 确认计划
   ↓
5. Codex 执行任务
   ↓
6. 验证结果
   ↓
7. 更新任务文件
```

### 示例：添加新功能

```bash
# 1. 创建任务计划
python tools/codex.py plan 添加批量下载功能

# 2. 编辑 tasks/todo.md 填写目标

# 3. 进入 Codex 计划模式
codex plan

# 4. Codex 生成计划后确认

# 5. Codex 执行任务

# 6. 验证功能

# 7. 更新 tasks/todo.md 和 tasks/lessons.md
```

---

## 🎯 最佳实践

### 1. Plan First

- 任何非平凡任务（3+ 步骤）先进入计划模式
- 出问题时立即停止并重新规划
- 写详细规格减少歧义

### 2. 使用子代理

- 复杂任务分派给子代理
- 保持主上下文干净
- 一个子代理专注一个任务

### 3. 自我改进

- 每次纠正后更新 `tasks/lessons.md`
- 写规则防止同样错误
- 会话开始复习相关 lessons

### 4. 验证后再完成

- 运行测试验证
- 检查日志
- 问自己："高级工程师会批准吗？"

### 5. 追求优雅

- 非平凡改动：暂停问"有更优雅的方案吗？"
- 如果修复感觉 hack：重新实现优雅方案
- 简单修复跳过，不要过度工程

---

## 🔍 常见问题

### Q: codex 命令找不到？

**A**: 
```bash
# 使用 npx
npx @anthropic-ai/codex [命令]

# 或安装
npm install -g @anthropic-ai/codex
```

### Q: 如何查看任务计划？

**A**: 
```bash
notepad D:\ideaworkspace\maomaochongapp\tasks\todo.md
```

### Q: 如何记录经验教训？

**A**: 
编辑 `D:\ideaworkspace\maomaochongapp\tasks\lessons.md`

### Q: 如何运行 gradle 命令？

**A**: 
```bash
cd D:\ideaworkspace\maomaochongapp
gradlew build
gradlew test
gradlew clean
```

---

## 📞 获取帮助

遇到问题可以：

1. 查看 `tools/codex.py help`
2. 查看 `AGENTS.md` 项目规范
3. 查看 `tasks/lessons.md` 经验教训
4. 询问 AI 助手

---

**开始使用**: `python tools/codex.py plan [任务名]`
