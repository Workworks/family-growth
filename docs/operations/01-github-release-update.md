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
- 所有正式版本证书相同；GitHub latest API 当前返回 v0.3.2 及其 `sha256:` digest。
- v0.3.0 的更新下载端存在 BUG-002，遇到“APK 下载或校验失败”时需手动安装 v0.3.1；v0.3.1 起具备三次有限重试、120 秒读取窗口和超时/DNS/TLS/存储分类提示。
- 真机验收顺序：手动安装 v0.3.1 → 创建少量本机数据 → 家长区检查到 v0.3.2 → 下载/校验 → 系统确认 → 检查版本与数据保留。

## 6. 验收步骤

1. 安装较低 `versionCode`、使用同一签名并配置同一仓库的 APK。
2. 发布更高 SemVer、`versionCode` 更高且签名相同的 Release APK。
3. 在家长模式点击“检查更新”，确认出现目标版本。
4. 下载后验证错误 digest/大小会被拒绝，正确资产才显示“打开系统安装界面”。
5. 首次按系统指引允许“安装未知应用”，返回后继续安装。
6. 覆盖安装后确认版本升级且应用数据保留。

真实覆盖升级必须在 Android 真机或平板上验收；仅有构建成功不能替代该证据。
