# Stage 19：GitHub Release 下载 0% 真机修复

状态：`COMPLETED`

日期：2026-08-26

产品 Phase：发布可靠性

需求：REQ-013、REQ-018、REQ-024、REQ-029；缺陷：BUG-002、BUG-004

## 目标与非目标

用户可观察目标：点击更新后立即看到“排队、连接、下载、暂停或校验”中的真实阶段；Android 系统下载服务负责网络切换和后台传输。若主入口长时间没有首字节，客户端自动切换到同一 GitHub Release 的官方 Asset API 入口；完成后仍校验大小和 SHA-256，再交给系统安装确认。

范围内：DownloadManager、进度/原因映射、首字节卡顿门禁、官方 Asset API 备用入口、缓存校验、取消、错误分类、JVM/lint/build、稳定签名、修复基线与更高热更新测试 Release。范围外：未知代理、关闭 digest/签名校验、静默安装、真实设备网络配置修改、保证被运营商完全阻断的 GitHub CDN 一定可达。

## 边界与不变量

- 更新元数据和 APK 仍来自配置仓库的 GitHub Release；备用 URL 必须是同仓库 `api.github.com/repos/{owner}/{repo}/releases/assets/{id}`。
- DownloadManager 成功不等于 APK 可信；必须在 App 私有路径重新计算大小/SHA-256，安装器仍校验包名、versionCode 和同签名。
- 排队/连接/暂停不显示为虚假的下载百分比；只有系统报告已接收字节时才显示百分比。
- 主入口 45 秒无任何字节后取消该系统任务并切换官方 API；备用入口失败给出具体系统原因，不无限重试。
- v0.3.1 已安装客户端无法被远程改写，用户必须手动安装一次修复基线；随后用更高版本完成真实热更新验证。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP19-1 | 已完成 | 需求/Bug/Spec 和 Stage 17 优先级交接 |
| WP19-2 | 已完成 | ReleaseAsset API URL 契约、DownloadManager 与校验缓存 |
| WP19-3 | 已完成 | 阶段/暂停/失败 UI、卡顿切换与取消 |
| WP19-4 | 已完成 | 双变体测试/lint/build、稳定签名、v0.3.3 修复基线和 v0.3.4 验证目标均已发布复验 |
| WP19-5 | 已完成 | 用户真机确认修复基线可正常更新到测试目标，BUG-004 关闭 |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V19-01 | JVM | 官方 Asset API URL、DownloadManager 状态/原因、阶段文案、digest | 非法入口拒绝；状态可解释；校验不降级 | Stage 19 evidence |
| V19-02 | Android | debug/release test、lint、build、aapt、apksigner | 两版构建通过且同稳定证书 | Stage 19 evidence |
| V19-03 | GitHub | 发布修复基线和更高测试目标，下载复验 | latest/digest/大小/版本/证书一致 | Stage 19 evidence |
| V19-04 | 真机 | 手动装修复基线，再应用内更新到测试目标 | 不长期停 0%；进入系统安装；数据保留 | 用户设备证据 |

## 完成标准

- [x] AC19-01 `PASS_OFFLINE`：DownloadManager 的排队/连接/暂停/下载/校验使用独立状态；收到字节前不显示百分比。
- [x] AC19-02 `PASS_OFFLINE`：45 秒无新字节取消主系统任务并切换同仓库 Asset API；两入口均失败后返回系统原因并提供 Release 页面。
- [x] AC19-03 `PASS_OFFLINE`：下载成功后从系统目录复制到 App 缓存前强制校验精确大小和 SHA-256；Android 系统继续执行包名、版本和同签名安装门禁。
- [x] AC19-04 `PASS`：22 项 debug/release JVM、lint/build通过；v0.3.3/v0.3.4 远端摘要、版本、包名和同证书复验通过。
- [x] AC19-05 `PASS_DEVICE`：用户确认真实设备可从 v0.3.3 正常更新到 v0.3.4，长期 0% 缺陷消失。旧数据逐项盘点未单独执行，继续由 Stage 11/14 的数据保留验收追踪，不作为 BUG-004 关闭证据。

## 安全检查、限制与交接

本 Stage 不加入第三方下载代理，不把 HTTPS 或 digest 校验降级。若用户网络同时阻断 GitHub Release CDN 的两个官方入口，新版会明确失败阶段，但仍需要用户调整网络或后续获得授权建设独立可信镜像。

2026-08-26 真机回执：用户明确反馈“可以正常更新了”。该证据证明 Stage 19 的检查、下载、校验和更新主问题已解除；未收到逐项数据保留盘点，因此不替代 Stage 11/14 对该项的独立验收。

## 交付

- 修复 commit：`54cdcb8`；验证目标 commit：`d2160a8`。
- 手动修复基线：[v0.3.3](https://github.com/Workworks/family-growth/releases/tag/v0.3.3)，APK SHA-256 `9dbb1e4617a04eb8f2f0f24b85ff2abe6ac28b75af969ac0157bda291bc38b85`。
- 热更新目标：[v0.3.4](https://github.com/Workworks/family-growth/releases/tag/v0.3.4)，APK SHA-256 `a0e359faa913a6b5b3b9482260d4653cef028d52b1d03be64d2996133f10bc3e`。
- Actions：`32949154719`（4m16s）、`32949579505`（4m28s）。
- 结构化证据：`docs/evidence/stage-19/acceptance.json`。
