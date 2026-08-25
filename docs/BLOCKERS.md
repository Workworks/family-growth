# 阻塞与限制清单

记录所有未达到 `COMPLETED` 的 Stage 及其缺少条件。**最后更新：2026-08-25。**

本清单区分两类：

- **外部阻塞**：缺少项目无法自行提供或未经授权影响共享环境的设备、仓库、凭据或第三方能力。代码可能已完成，但真实验收无法执行；不得用 Mock、构建或文档代替。
- **工程未完成**：能力在本项目范围内，只是尚未建设，继续按 Stage 开发即可。

状态事实以对应 Stage 报告为准；本文件只集中描述缺口、解除条件和推进方。

## 1. 外部阻塞

### Stage 2：工程骨架与 Family / Growth 基础

状态：`BLOCKED`　文档：[Stage 2](stages/stage-2-report.md)

已完成：后端模块、Family/Parent/Child/GrowthPlan/GrowthGoal/GrowthTask、Flyway/JPA、基础 REST API、PostgreSQL 16.15 验证、Android 单测/lint/debug APK 构建。

缺少：Android 平板或可用模拟器上的安装、启动、横竖屏和截图证据。本机 Emulator 37.1.11 同版本官方重装后仍无法建立可用 ADB。

解除条件（任一）：

1. 连接启用 USB 调试的 Android 8+ 真机/平板并授权 ADB；或
2. 用户明确授权重装共享的 API 34 Google APIs x86_64 system image，再创建项目专用 AVD 回放。

推进方：用户提供设备或共享 SDK 修改授权；Agent 执行回放并留证据。

### Stage 13：基础体验宽度与 Android 前端

状态：`BLOCKED`　文档：[Stage 13](stages/stage-13-report.md)

已完成：本地基础闭环、五区响应式前端、自动化和 0.2.0 debug APK。

缺少：真机/平板上的安装启动、真实触控交互、PIN、防沉迷分钟推进和重启持久化证据。

解除条件：连接启用 USB 调试的 Android 8+ 真机/平板并授权 ADB；可与 Stage 2 使用同一设备回放。

推进方：用户提供设备；Agent 安装并按 Stage 13 V13-04/V13-05 回放留证。

### Stage 11：GitHub Release APK 更新与当前版本交付

状态：`BLOCKED`　文档：[Stage 11](stages/stage-11-report.md)

已完成：GitHub latest release client、SemVer、固定资产、HTTPS/大小/SHA-256 校验、FileProvider、系统安装引导、tag 发布 workflow、0.1.1 debug APK。

已解除：公开仓库 `Workworks/family-growth` 已创建并推送 `main`。

缺少：稳定 release 签名 secrets、两版 Release 资产和可用 Android 设备上的同签名覆盖升级/数据保留证据。

解除条件：用户确认现有 release keystore，或授权生成新证书并指定长期备份位置；随后连接 Android 真机/平板，使用同一证书发布低/高两个 versionCode，回放检查、下载、系统确认、覆盖升级和数据保留。

推进方：用户确认签名备份并提供设备；Agent 配置、发布和验收。签名私钥不得进入聊天或 Git。

### Stage 14：公开仓库与真实 Release 更新链

状态：`BLOCKED`　文档：[Stage 14](stages/stage-14-report.md)

已完成：PUBLIC 仓库 `Workworks/family-growth`、五个分 Stage commits、origin/main 推送、Android 默认更新源绑定和重新构建的 0.2.0 debug 基线。

缺少：稳定 Android release keystore 与长期备份决定；GitHub 当前无四项签名 Secrets，也无 Release。另需 Android 设备验收覆盖升级。

解除条件：用户提供现有 keystore 的本机路径和 alias（密码不得发在聊天，可由本机安全输入），或明确授权 Agent 生成新证书并指定长期备份目录；然后由 Agent 配置 GitHub Secrets、发布递增版本并回放。

推进方：用户确认签名方案/备份和设备；Agent 完成 CI Release 与验收。

## 2. 工程未完成

### Stage 3–10：V1 生产服务端深度业务

状态：`NOT_STARTED`　文档：[Stage 路线图](stages/stage-roadmap.md)

Stage 13 已提供这些领域的 Android 本机基础体验，但尚未建设生产级服务端任务完成域、事务 Wallet/Ledger、认证/RBAC、多端同步、复杂配置、跨域报告和 V1 端到端总验收。

解除方式：按 Stage 3→10 的 Spec 顺序继续工程开发；每个 Stage 在进入 `IN_PROGRESS` 前先补齐目标、边界、验证和编号完成标准。

推进方：Agent；涉及产品默认值、部署宿主和真实设备时由用户确认或提供条件。

## 3. 当前已知限制

- 现有 Android APK 是 debug 签名测试包，未绑定未知的 GitHub 仓库，不能称为正式发行包。
- 现有基础 API 尚未实现生产认证与家长 PIN，不可直接部署为可用产品。
- AGP 8.7.3 对 compileSdk 36 有兼容警告；构建通过，但应在后续 Android 依赖 Stage 对齐。
- H2 只用于隔离测试；正式验收数据库基线是 PostgreSQL。

跨 Stage 的长期限制同步见 [已知限制](acceptance/16-known-limitations.md)。
