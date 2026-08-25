# 故障排查

## 后端构建失败

使用 Java 17 和仓库 `mvnw.cmd`；定向模块测试必须带 `-am`。检查完整 reactor 输出，不用本地旧构件推断结论。

## PostgreSQL 迁移失败

核对数据库版本、连接变量和 Flyway 历史；不得修改已执行 migration 或清空真实数据绕过。

## Android 构建失败

核对 JDK 17、Android SDK、Gradle Wrapper 和 AGP/compileSdk 警告。生成 APK 后仍需设备安装验证。

## Emulator offline

现有环境问题见 [BLOCKERS](../BLOCKERS.md)。不要清除用户 AVD；可连接真机，或在明确授权后重装共享 system image。

## 检查更新显示未配置

当前 APK 未绑定仓库是预期安全状态。按 [GitHub Release 发布手册](01-github-release-update.md)提供 `GITHUB_REPOSITORY=owner/repo` 重新构建。
