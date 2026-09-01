# GitHub Release APK 更新与发布手册

## 1. 更新契约

- 仓库：默认公开仓库 `Workworks/family-growth`；构建参数 `GITHUB_REPOSITORY=owner/repo` 仍可覆盖。
- Release tag：`vMAJOR.MINOR.PATCH`，例如 `v0.1.2`。
- APK asset：`family-growth-MAJOR.MINOR.PATCH.apk`，例如 `family-growth-0.1.2.apk`。
- 客户端只接受配置仓库的 GitHub HTTPS 下载地址，并要求 GitHub API 返回 `sha256:<64 hex>` digest。
- 只有 APK 大小和 SHA-256 均匹配，客户端才通过 `FileProvider` 把私有缓存文件交给 Android 系统安装界面。
- 覆盖升级必须保持 `applicationId=com.familygrowth.android`、使用同一签名证书，并递增 `versionCode`。

这是一种 APK 应用内更新，不是动态替换 dex/resources 的无感热修复。Android 系统安装确认不可绕过。

## 2. 首次配置

1. 把项目推送到目标公开 GitHub 仓库。
2. 生成并离线保管 release keystore；不得提交 keystore 或密码。
3. 在仓库 Actions secrets 中配置：
   - `ANDROID_KEYSTORE_BASE64`：keystore 文件的 Base64 内容；
   - `ANDROID_STORE_PASSWORD`；
   - `ANDROID_KEY_ALIAS`；
   - `ANDROID_KEY_PASSWORD`。
4. 确保每次发布前同步修改 `family-growth-android/app/build.gradle.kts` 中的 `versionName` 与递增的 `versionCode`。

当前仓库已经配置：私钥备份在 `E:\FamilyGrowthSigningBackup`，恢复记录单独保存在当前 Windows 用户目录；GitHub 四项 Secrets 已存在。证书 SHA-256 为 `9179A5DA2973E8FF9115EDD1FB74E21AD70D9540C1D33203C9AF442106D2EACB`。恢复记录包含密码，不得提交、截图或发送到聊天；应尽快转存到密码管理器并与私钥分开备份。

## 3. 发布

```powershell
git tag v0.1.2
git push origin v0.1.2
```

`.github/workflows/android-release.yml` 会校验 tag 与 `versionName` 一致，执行 release 单测、lint 和构建，使用 secrets 中的稳定证书签名，并创建不可覆盖同名 asset 的 GitHub Release。任一签名 secret 缺失、测试失败或 Release 已存在都会失败。

## 4. 本地构建更新源

```powershell
cd family-growth-android
.\gradlew.bat clean assembleDebug
```

当前默认绑定 `Workworks/family-growth`。若显式传入空值或非法仓库，家长端 fail-closed 且不请求占位 URL。不要把 GitHub Token 写入 APK；V1 仅支持公开仓库。

## 5. 已发布更新链

- 基线：[v0.2.0](https://github.com/Workworks/family-growth/releases/tag/v0.2.0)，versionCode 3，asset `family-growth-0.2.0.apk`。
- 更新：[v0.2.1](https://github.com/Workworks/family-growth/releases/tag/v0.2.1)，versionCode 4，asset `family-growth-0.2.1.apk`。
- 儿童适龄更新：[v0.2.2](https://github.com/Workworks/family-growth/releases/tag/v0.2.2)，versionCode 5，asset `family-growth-0.2.2.apk`。
- 生产闭环更新：[v0.3.0](https://github.com/Workworks/family-growth/releases/tag/v0.3.0)，versionCode 6。
- 下载修复与儿童图标：[v0.3.1](https://github.com/Workworks/family-growth/releases/tag/v0.3.1)，versionCode 7，asset `family-growth-0.3.1.apk`。
- 双视角、奖励浏览与教学视频：[v0.3.2](https://github.com/Workworks/family-growth/releases/tag/v0.3.2)，versionCode 8，asset `family-growth-0.3.2.apk`。
- 系统下载器修复基线：[v0.3.3](https://github.com/Workworks/family-growth/releases/tag/v0.3.3)，versionCode 9。
- 热更新验证目标：[v0.3.4](https://github.com/Workworks/family-growth/releases/tag/v0.3.4)，versionCode 10。
- Stage 20/21 教学基座与可靠同步：[v0.3.5](https://github.com/Workworks/family-growth/releases/tag/v0.3.5)，versionCode 11，asset `family-growth-0.3.5.apk`。
- 小学分段与自主学习首切：[v0.3.6](https://github.com/Workworks/family-growth/releases/tag/v0.3.6)，versionCode 12，asset `family-growth-0.3.6.apk`。
- 初中/高中深度与连续性、隐私、防沉迷收口：[v0.3.7](https://github.com/Workworks/family-growth/releases/tag/v0.3.7)，versionCode 13，asset `family-growth-0.3.7.apk`。
- Stage 26 完成性修复：[v0.3.8](https://github.com/Workworks/family-growth/releases/tag/v0.3.8)，versionCode 14，asset `family-growth-0.3.8.apk`。
- Stage 27–32 深化、可靠同步、家庭经济实验室与工具链治理：[v0.3.9](https://github.com/Workworks/family-growth/releases/tag/v0.3.9)，versionCode 15，asset `family-growth-0.3.9.apk`。
- 所有正式版本证书相同；GitHub latest 当前为 v0.3.9，公开 digest 与下载 SHA-256 均为 `sha256:846fae8b32e8cd68e0b3e0a20bbe636f314e31b2e41b95a2be97c79e4f3232eb`，大小 12,065,826 字节；workflow `33508682095` 成功。
- v0.3.0–v0.3.2 的客户端都不含 Stage 19 系统下载器；遇到失败或长期 0% 时必须手动覆盖安装 v0.3.3 一次，不能期待旧客户端远程获得修复代码。
- 真机验收顺序：已有 v0.3.8 正式签名版本的设备不卸载、不清数据，在家长区检查到 v0.3.9 → 记录排队/连接/下载/暂停/校验阶段 → 系统确认 → 检查版本、课程、账本、隐私配置、Room/Keystore 和加密待办保留。仍低于 v0.3.3 时先手动覆盖 v0.3.3 修复基线。

## 6. 验收步骤

1. 安装较低 `versionCode`、使用同一签名并配置同一仓库的 APK。
2. 发布更高 SemVer、`versionCode` 更高且签名相同的 Release APK。
3. 在家长模式点击“检查更新”，确认出现目标版本。
4. 下载后验证错误 digest/大小会被拒绝，正确资产才显示“打开系统安装界面”。
5. 首次按系统指引允许“安装未知应用”，返回后继续安装。
6. 覆盖安装后确认版本升级且应用数据保留。

真实覆盖升级必须在 Android 真机或平板上验收；仅有构建成功不能替代该证据。
