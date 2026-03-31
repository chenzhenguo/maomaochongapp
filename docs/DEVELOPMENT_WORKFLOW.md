# 毛毛虫资源管家 - 完整开发流程指南

本文档详细描述了从开发、测试、编译、验证到GitHub自动编译提交的完整流程。

## 1. 开发环境准备

### 1.1 系统要求
- JDK 17
- Android SDK (API level 35)
- Android Studio 或 IntelliJ IDEA
- Git

### 1.2 项目克隆和初始化
```bash
git clone <repository-url>
cd maomaochongapp
./gradlew --version  # 验证Gradle版本
```

## 2. 开发流程

### 2.1 功能开发步骤
1. **需求分析** - 查看PRD文档 (`docs/PRD.md`)
2. **设计规划** - 参考设计文档 (`docs/DESIGN.md`)
3. **创建分支** - 基于main分支创建功能分支
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **TDD开发** - 遵循测试驱动开发原则
   - 先编写测试用例
   - 实现功能代码
   - 运行测试验证

### 2.2 代码规范
- 使用Kotlin idiomatic风格
- MVVM架构模式
- 不可变数据原则
- 文件大小控制在800行以内
- 函数长度控制在50行以内

## 3. 测试流程

### 3.1 本地测试命令

#### 单元测试 (Unit Tests)
```bash
# 运行所有单元测试
./gradlew testDebugUnitTest

# 运行特定测试类
./gradlew testDebugUnitTest --tests "com.maomaochongapp.renamer.RenamePlannerTest"

# 生成测试覆盖率报告
./gradlew jacocoTestReport
```

#### Android仪器化测试 (Instrumentation Tests)
```bash
# 运行Android测试（需要连接设备或启动模拟器）
./gradlew connectedDebugAndroidTest

# 运行特定E2E测试
./gradlew connectedDebugAndroidTest --tests "*PictureBookCreationE2ETest"
```

### 3.2 测试目录结构
```
app/src/test/           # JVM单元测试
├── java/com/maomaochongapp/
│   ├── renamer/        # 批量重命名相关测试
│   ├── sequence/       # 序号管理相关测试
│   └── picturebook/    # 绘本功能相关测试

app/src/androidTest/    # Android仪器化测试
├── java/com/maomaochongapp/
│   ├── picturebook/
│   │   ├── data/local/ # 数据库测试
│   │   └── e2e/        # 端到端测试
```

### 3.3 测试覆盖率要求
- **最低覆盖率**: 80%
- **测试类型**:
  - 单元测试 (Unit Tests)
  - 集成测试 (Integration Tests)
  - 端到端测试 (E2E Tests)

## 4. 编译和构建

### 4.1 本地构建命令

#### Debug版本构建
```bash
# 构建Debug APK
./gradlew assembleDebug

# 构建并安装到连接的设备
./gradlew installDebug
```

#### Release版本构建
```bash
# 构建Release APK
./gradlew assembleRelease

# 构建Release AAB (Android App Bundle)
./gradlew bundleRelease
```

### 4.2 构建产物位置
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Release AAB**: `app/build/outputs/bundle/release/app-release.aab`

### 4.3 代码质量检查
```bash
# 运行Android Lint检查
./gradlew lintDebug

# 运行所有检查任务
./gradlew check
```

## 5. 本地验证流程

### 5.1 完整验证命令序列
```bash
# 1. 清理项目
./gradlew clean

# 2. 运行Lint检查
./gradlew lintDebug

# 3. 运行单元测试
./gradlew testDebugUnitTest

# 4. 构建Debug APK
./gradlew assembleDebug

# 5. 验证构建成功（检查APK是否存在）
ls -la app/build/outputs/apk/debug/
```

### 5.2 验证清单
- [ ] 代码编译无错误
- [ ] Lint检查通过
- [ ] 单元测试全部通过
- [ ] APK成功生成
- [ ] 应用在设备上正常运行
- [ ] 新功能按预期工作
- [ ] 无内存泄漏或性能问题

## 6. GitHub提交和CI/CD流程

### 6.1 提交前准备
```bash
# 1. 检查状态
git status

# 2. 添加修改的文件
git add .

# 3. 运行本地验证（确保通过）
./gradlew clean lintDebug testDebugUnitTest assembleDebug

# 4. 提交代码
git commit -m "feat: your feature description"
```

### 6.2 Pull Request流程
1. **推送分支到GitHub**
   ```bash
   git push origin feature/your-feature-name
   ```

2. **创建Pull Request**
   - 目标分支: `main`
   - 包含完整的变更描述
   - 添加测试计划

3. **CI自动触发**
   - GitHub Actions会自动运行CI工作流
   - 包括: Lint检查、单元测试、Debug APK构建
   - 构建产物会作为Artifacts上传

### 6.3 CI工作流详情

#### 自动触发条件
- Push到main分支
- Pull Request到main分支
- 手动触发 (workflow_dispatch)

#### CI执行步骤
1. **环境设置**: JDK 17 + Android SDK 35
2. **依赖缓存**: Gradle依赖缓存优化
3. **Lint检查**: `./gradlew lintDebug`
4. **单元测试**: `./gradlew testDebugUnitTest`
5. **Debug构建**: `./gradlew assembleDebug`
6. **产物上传**: Debug APK作为Artifacts保存7天
7. **自动发布**: main分支push时自动创建预发布版本

### 6.4 Release流程

#### 创建正式版本
1. **打标签**
   ```bash
   git tag -a v1.1.0 -m "Release version 1.1.0"
   git push origin v1.1.0
   ```

2. **自动Release工作流**
   - 触发Release CI工作流
   - 构建Release APK和AAB
   - 自动生成Release Notes
   - 上传到GitHub Releases

## 7. 常见问题解决

### 7.1 编译错误解决
- 使用 `./gradlew build --stacktrace` 查看详细错误
- 检查JDK版本是否为17
- 验证Android SDK是否包含API 35

### 7.2 测试失败排查
- 检查测试依赖是否正确配置
- 确认Mock对象配置正确
- 查看测试报告: `app/build/reports/tests/`

### 7.3 CI失败处理
- 下载诊断Artifacts进行分析
- 在本地复现CI环境
- 检查网络依赖和权限配置

## 8. 最佳实践

### 8.1 开发最佳实践
- **小步提交**: 每次提交只包含一个逻辑变更
- **测试先行**: 遵循TDD原则
- **代码审查**: 所有代码必须经过审查
- **文档同步**: 代码变更时同步更新文档

### 8.2 性能优化
- 避免在主线程进行耗时操作
- 使用协程处理异步任务
- 优化图片加载和内存使用
- 合理使用Room数据库事务

### 8.3 安全考虑
- 不要硬编码敏感信息
- 使用SAF (Storage Access Framework) 安全访问文件
- 验证所有用户输入
- 遵循Android安全最佳实践

---

**注意**: 本流程基于项目现有的配置和最佳实践制定。如有疑问，请参考项目中的具体实现或联系项目维护者。