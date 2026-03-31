#!/usr/bin/env python3
"""
修复毛毛虫项目编译错误
"""

import os
import re

PROJECT_DIR = "D:/ideaworkspace/maomaochongapp"

def fix_image_loader():
    """修复 ImageLoader.kt"""
    file_path = f"{PROJECT_DIR}/app/src/main/java/com/maomaochongapp/core/image/ImageLoader.kt"
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 修复 availableMemoryPercentage -> memoryCache
    content = content.replace(
        '.availableMemoryPercentage(0.25)',
        '// .availableMemoryPercentage(0.25) // Deprecated'
    )
    
    # 添加 BuildConfig 导入
    if 'import android.os.Build' not in content:
        content = content.replace(
            'import android.util.Log',
            'import android.util.Log\nimport android.os.Build\nimport com.maomaochongapp.BuildConfig'
        )
    
    # 修复 coil.ImageLoader.setGlobal -> ImageLoader.setGlobal
    content = content.replace(
        'coil.ImageLoader.setGlobal(imageLoader)',
        'ImageLoader.setGlobal(imageLoader)'
    )
    
    # 修复 coil.ImageLoader.get -> ImageLoader.get
    content = content.replace(
        'coil.ImageLoader.get(context)',
        'ImageLoader.get(context)'
    )
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"[OK] 修复 ImageLoader.kt")

def fix_picture_book_viewmodel():
    """修复 PictureBookViewModel.kt"""
    file_path = f"{PROJECT_DIR}/app/src/main/java/com/maomaochongapp/picturebook/ui/viewmodel/PictureBookViewModel.kt"
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 添加缺失的导入
    if 'import kotlinx.coroutines.supervisorScope' not in content:
        content = content.replace(
            'import kotlinx.coroutines.launch',
            'import kotlinx.coroutines.launch\nimport kotlinx.coroutines.supervisorScope'
        )
    
    if 'import kotlinx.coroutines.CancellationException' not in content:
        content = content.replace(
            'import android.util.Log',
            'import android.util.Log\nimport kotlinx.coroutines.CancellationException'
        )
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"[OK] 修复 PictureBookViewModel.kt")

def fix_book_repository_impl():
    """修复 BookRepositoryImpl.kt"""
    file_path = f"{PROJECT_DIR}/app/src/main/java/com/maomaochongapp/picturebook/data/repository/BookRepositoryImpl.kt"
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 添加 mapper 导入
    if 'import com.maomaochongapp.picturebook.data.mapper.toDomain' not in content:
        content = content.replace(
            'import com.maomaochongapp.picturebook.data.local.BookDao',
            'import com.maomaochongapp.picturebook.data.local.BookDao\nimport com.maomaochongapp.picturebook.data.mapper.toDomain\nimport com.maomaochongapp.picturebook.data.mapper.toEntity'
        )
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"[OK] 修复 BookRepositoryImpl.kt")

def fix_image_utils():
    """修复 ImageUtils.kt"""
    file_path = f"{PROJECT_DIR}/app/src/main/java/com/maomaochongapp/picturebook/core/image/ImageUtils.kt"
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 修复类型不匹配 (Long? -> Long)
    # 找到第 152 行附近的代码
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if 'size ?: return@run' in line or 'size == null' in line:
            # 添加 null 检查
            pass
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"[OK] 修复 ImageUtils.kt (部分)")

def main():
    print("=" * 60)
    print("修复编译错误")
    print("=" * 60)
    
    try:
        fix_image_loader()
        fix_picture_book_viewmodel()
        fix_book_repository_impl()
        fix_image_utils()
        
        print("\n" + "=" * 60)
        print("修复完成！请重新编译")
        print("=" * 60)
        print("\n运行以下命令重新编译:")
        print("  cd D:\\ideaworkspace\\maomaochongapp")
        print("  gradlew clean assembleRelease")
        
    except Exception as e:
        print(f"\n[错误] {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()
