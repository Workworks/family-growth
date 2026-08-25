# Stage 11：GitHub Release APK 热更新与当前版本交付

状态：`BLOCKED`

日期：2026-08-25

产品 Phase：计划外发布能力；全局编号 11 不代表跳过 Stage 2–10 的业务顺序

需求：REQ-013

## 目标与非目标

目标：家长可在 Android App 内检查配置仓库的最新 GitHub Release；发现更高语义版本后下载对应 APK，使用 GitHub Release asset 的 SHA-256 digest 做完整性校验，再交给 Android 系统安装界面，由用户确认覆盖更新。同时生成并交付本轮可安装 debug APK。

非目标：不静默安装、不绕过“允许来自此来源的应用”设置、不在 APK 内保存 GitHub Token、不支持私有仓库、不在缺少 remote/secrets 时擅自创建真实 Release、不宣称 debug 签名包为正式生产包。

## 前置与边界

- 当前仓库没有 GitHub remote，`GITHUB_REPOSITORY=owner/repo` 必须由构建环境提供；未配置时界面明确报错且不发网络请求。
- V1 更新源只支持公开 GitHub 仓库；GitHub 官方 latest release API 可匿名读取公开资源。
- Release tag 约定 `vMAJOR.MINOR.PATCH`，APK asset 约定 `family-growth-MAJOR.MINOR.PATCH.apk`。
- APK asset 必须包含 GitHub 返回的 `sha256:<64 hex>` digest；缺失、格式错误或校验不一致时 fail-closed，不启动安装。
- Android 覆盖升级必须保持 applicationId、递增 versionCode 和相同签名证书。系统安装确认是安全边界，App 不绕过。
- debug APK 仅供当前内部测试；正式 Release 需稳定且安全保存的发布签名证书。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP11-1 | 完成 | GitHub Release 配置、API client、SemVer 与 asset/digest 校验 |
| WP11-2 | 完成 | 下载、SHA-256、FileProvider、系统安装权限与确认 |
| WP11-3 | 完成 | 家长端更新 UI、错误/进度/未配置状态 |
| WP11-4 | 完成 | 单测、lint、构建、APK 哈希、Actions 工作流与发布说明 |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V11-01 | JVM | SemVer、asset 选择、digest 格式测试 | 旧版/同版不提示；缺 digest 或错名拒绝 | Stage 11 单测 |
| V11-02 | Android build | 单测、lint、assembleDebug | 全部退出 0并生成 0.1.1 APK | `evidence/stage-11/acceptance.json` |
| V11-03 | 未配置构建 | 点击检查更新 | 显示更新源未配置，不请求占位 URL | UI/单测 |
| V11-04 | 真实公开 GitHub Release | 检查、下载、校验、系统安装确认 | 只在 digest 匹配后打开系统确认页 | 需仓库和 Android 设备；缺失则 BLOCKED |
| V11-05 | 覆盖升级 | 用同一签名的更高 versionCode APK 更新 | 用户确认后版本升级、数据保留 | 需两版发布资产和 Android 设备 |

## 完成标准

- [x] AC11-01 `PASS`：latest release、SemVer、精确 asset 名称、仓库 URL 和 digest 契约已实现；JVM 测试通过。
- [x] AC11-02 `PASS`：APK 仅下载到 `cacheDir/updates`，大小/digest 失败删除 `.part` 和目标文件。
- [x] AC11-03 `PASS`：FileProvider、`REQUEST_INSTALL_PACKAGES`、未知来源设置和系统安装确认已实现，无静默安装路径。
- [x] AC11-04 `PASS（实现门禁）`：家长端包含未配置、无更新、检查、可更新、下载、失败、权限和待安装状态；运行态归入 AC11-06。
- [x] AC11-05 `PASS`：0.1.1 / versionCode 2 debug APK 已构建、v2 签名验证并交付，SHA-256 见证据。
- [ ] AC11-06 `BLOCKED`：当前仓库无 GitHub remote/Release，且本机 Android Emulator 仍不可用；真实下载和两版本覆盖升级未执行。

## 实施结果

- 客户端调用公开仓库 `/repos/{owner}/{repo}/releases/latest`，只选择 `family-growth-{version}.apk`。
- 初始和重定向后的下载地址均限制为 GitHub HTTPS 主机；APK 最大 250 MiB，大小与 GitHub digest 双重校验。
- `.github/workflows/android-release.yml` 在 `vMAJOR.MINOR.PATCH` tag 上执行 release 测试、lint、稳定证书签名和 Release 创建；缺 secret 或 tag/version 不一致即失败。
- 发布/配置/验收步骤见 `../operations/01-github-release-update.md`。
- 本地交付：`../../dist/family-growth-0.1.1-debug.apk`；这是未配置仓库的 debug 签名包，不冒充正式 Release。

## 验证结论

| 验证项 | 结果 | 说明 |
| --- | --- | --- |
| V11-01 | `PASS` | 4 个更新契约测试与既有 1 个 AppMode 测试通过，0 failure / 0 error |
| V11-02 | `PASS` | `testDebugUnitTest lintDebug assembleDebug` 退出 0 |
| V11-03 | `PASS（实现门禁）` | 空/URL 形式仓库配置被拒绝；空配置不构造 client，UI 显示未配置 |
| V11-04 | `BLOCKED` | 没有真实公开 Release 和可运行 Android 设备 |
| V11-05 | `BLOCKED` | 没有同签名的两版 Release 资产和可运行 Android 设备 |

自动化、APK 与签名证据见 `../evidence/stage-11/acceptance.json`。Stage 保持 `BLOCKED`，直到真实 GitHub Release 和 Android 真机/平板完成覆盖升级回放。

## 安全检查

下载仅允许 GitHub API 返回的 HTTPS asset URL；不接受 UI 输入任意下载地址。公开仓库不携带 Token。digest 必须匹配后才授予单个缓存 APK 的临时读取权限。发布签名私钥不得进入 Git、日志、Release 附件或证据。
