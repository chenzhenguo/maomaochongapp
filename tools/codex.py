#!/usr/bin/env python3
"""
Codex 命令执行工具 - 毛毛虫项目

用法:
    python codex.py [命令] [参数...]

示例:
    python codex.py plan          - 进入计划模式
    python codex.py build         - 构建项目
    python codex.py test          - 运行测试
    python codex.py run [task]    - 执行任务
    python codex.py help          - 显示帮助
"""

import os
import sys
import subprocess
from pathlib import Path

# 项目根目录
PROJECT_DIR = Path(__file__).parent.parent
TASKS_DIR = PROJECT_DIR / "tasks"

def print_banner():
    print("=" * 70)
    print("毛毛虫项目 - Codex 执行工具")
    print("=" * 70)
    print(f"项目目录：{PROJECT_DIR}")
    print("=" * 70)

def check_codex():
    """检查 codex 是否可用"""
    try:
        result = subprocess.run(['codex', '--version'], capture_output=True, text=True, timeout=10)
        if result.returncode == 0:
            print(f"[OK] Codex 已安装：{result.stdout.strip()}")
            return True
    except:
        pass
    
    print("[警告] Codex CLI 未找到")
    print("\n安装方法:")
    print("  npm install -g @anthropic-ai/codex")
    print("\n或使用 npx 运行:")
    print("  npx @anthropic-ai/codex [命令]")
    return False

def run_codex_command(args):
    """运行 codex 命令"""
    cmd = ['codex'] + args
    
    try:
        result = subprocess.run(cmd, cwd=PROJECT_DIR, capture_output=True, text=True, timeout=None)
        
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print(result.stderr, file=sys.stderr)
        
        return result.returncode
    except FileNotFoundError:
        print("[错误] 未找到 codex 命令")
        print("\n尝试使用 npx...")
        return run_npx_codex(args)
    except Exception as e:
        print(f"[错误] {e}")
        return 1

def run_npx_codex(args):
    """使用 npx 运行 codex"""
    cmd = ['npx', '@anthropic-ai/codex'] + args
    
    try:
        result = subprocess.run(cmd, cwd=PROJECT_DIR, capture_output=True, text=True, timeout=None)
        
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print(result.stderr, file=sys.stderr)
        
        return result.returncode
    except Exception as e:
        print(f"[错误] {e}")
        return 1

def create_task_plan(task_name):
    """创建任务计划"""
    TASKS_DIR.mkdir(parents=True, exist_ok=True)
    
    todo_file = TASKS_DIR / "todo.md"
    
    plan_content = f"""# 任务：{task_name}

**创建时间**: {subprocess.run(['date'], capture_output=True, text=True).stdout.strip()}
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
"""
    
    with open(todo_file, 'w', encoding='utf-8') as f:
        f.write(plan_content)
    
    print(f"[OK] 任务计划已创建：{todo_file}")
    return todo_file

def show_help():
    """显示帮助信息"""
    help_text = """
Codex 命令执行工具 - 毛毛虫项目

用法:
    python codex.py [命令] [参数...]

可用命令:
    plan [任务名]     - 进入计划模式，创建任务计划
    build            - 构建项目 (gradle build)
    test             - 运行测试 (gradle test)
    run [任务]       - 执行任务
    clean            - 清理项目
    help             - 显示帮助

示例:
    python codex.py plan 添加下载功能
    python codex.py build
    python codex.py test
    python codex.py run 修复 bug

直接模式:
    其他参数将直接传递给 codex 命令

注意:
    - 需要安装 Codex CLI 或使用 npx
    - 项目目录：D:\\ideaworkspace\\maomaochongapp
"""
    print(help_text)

def main():
    print_banner()
    print()
    
    if len(sys.argv) < 2:
        show_help()
        return
    
    command = sys.argv[1].lower()
    args = sys.argv[2:]
    
    if command == 'help':
        show_help()
        return
    
    if command == 'plan':
        task_name = ' '.join(args) if args else '新任务'
        create_task_plan(task_name)
        print("\n现在可以使用 codex 进入计划模式:")
        print(f"  codex plan -t \"{task_name}\"")
        return
    
    if command == 'build':
        print("构建项目...")
        subprocess.run(['gradlew', 'build'], cwd=PROJECT_DIR)
        return
    
    if command == 'test':
        print("运行测试...")
        subprocess.run(['gradlew', 'test'], cwd=PROJECT_DIR)
        return
    
    if command == 'clean':
        print("清理项目...")
        subprocess.run(['gradlew', 'clean'], cwd=PROJECT_DIR)
        return
    
    # 其他命令直接传递给 codex
    exit_code = run_codex_command(sys.argv[1:])
    sys.exit(exit_code)

if __name__ == '__main__':
    main()
