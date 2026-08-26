# 阻塞与限制清单

记录所有未达到 `COMPLETED` 的 Stage 及其缺少条件。**最后更新：2026-08-26。**

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

已解除：稳定 release keystore/Secrets 与 v0.2.0、v0.2.1、v0.2.2 三版同签名 Release 均已完成，公开 digest 和下载签名复验通过。

缺少：可用 Android 设备上的同签名覆盖升级/数据保留证据。

解除条件：连接 Android 真机/平板，先安装 v0.2.1，再在 App 内检查/下载/校验并由系统确认更新到 v0.2.2，检查数据保留。

推进方：用户提供设备；Agent 回放验收。签名私钥不得进入聊天或 Git。

### Stage 14：公开仓库与真实 Release 更新链

状态：`BLOCKED`　文档：[Stage 14](stages/stage-14-report.md)

已完成：PUBLIC 仓库 `Workworks/family-growth`、五个分 Stage commits、origin/main 推送、Android 默认更新源绑定和重新构建的 0.2.0 debug 基线。

已解除：用户已授权生成新 release keystore，并指定私钥备份目录 `E:\FamilyGrowthSigningBackup`。

已完成：新 release keystore、分离恢复记录、私有 ACL、四项 GitHub Secrets 和本地 release 门禁/指纹比对。

已完成：v0.2.0、v0.2.1 与 Stage 15 v0.2.2 tag workflow、公开 Release、精确 APK asset、GitHub digest、版本递增和同证书下载复验。

缺少：Android 设备验收覆盖升级。

解除条件：用户提供 Android 设备，Agent 回放 v0.2.1→v0.2.2 更新与数据保留。

推进方：用户提供设备；Agent 完成系统安装确认与数据保留验收。

### Stage 8：Android 生产 API 接入

状态：`BLOCKED`　文档：[Stage 8](stages/stage-8-report.md)

已完成：生产 URL 策略、内存 Token、父/子会话、任务/钱包同步、孩子提交、家长确认、401 恢复状态、后端 sync API、JVM/lint/debug/release 构建。

缺少：Android 平板上的真实 HTTPS/局域网连接、触控登录同步、断网/401 恢复、旋转和无障碍回放。

解除条件：连接启用 USB 调试的 Android 平板/真机，并提供可从设备访问的家庭服务地址。

推进方：用户提供设备/可访问服务；Agent 回放并留证。

## 2. 工程未完成

### Stage 15：3 岁起儿童端发展适龄改造

状态：`BLOCKED`　文档：[Stage 15](stages/stage-15-report.md)

已完成：长期准则、发展依据、CHILD 三入口/单任务/简化财商、13 项 JVM 测试、debug/release lint/构建和稳定签名 0.2.2 APK。

缺少：平板横屏真实触控、TalkBack、字体放大、Reduced Motion 和温和限时退出回放。

解除方式：用户提供设备后完成真实可用性验收。

推进方：用户提供最终设备条件；Agent 回放并留证据。

### Stage 3–10：V1 生产服务端深度业务

状态：Stage 3–7/9 `COMPLETED`，Stage 8 `BLOCKED`，Stage 10 `IN_PROGRESS`　文档：[Stage 路线图](stages/stage-roadmap.md)

生产认证、任务审核、Wallet/Ledger、兑换、商店/储蓄/愿望、纯模拟基金和跨域报告已完成；剩余工程范围是 Stage 10 部署、恢复、发布和总验收。

当前推进：REQ-020/REQ-022 已启动 Stage 3–10；Stage 3–7/9 在 PostgreSQL 通过；Stage 8 API/Android 离线门禁已过，缺平板真实登录/同步/写入回放；Stage 10 正在推进。

解除方式：按 Stage 3→10 的 Spec 顺序继续工程开发；每个 Stage 仅在前序门禁通过后进入 `IN_PROGRESS`。

推进方：Agent；涉及产品默认值、部署宿主和真实设备时由用户确认或提供条件。

## 3. 当前已知限制

- 现有 v0.2.0/v0.2.1/v0.2.2 APK 是稳定 release 签名包；历史 debug APK 仅供内部证据，不能与 release 包互相覆盖。
- Stage 3 已实现生产认证/PIN/RBAC；家庭局域网正式部署仍需可信 TLS、备份和 Stage 4–10 业务闭环。
- AGP 8.7.3 对 compileSdk 36 有兼容警告；构建通过，但应在后续 Android 依赖 Stage 对齐。
- H2 只用于隔离测试；正式验收数据库基线是 PostgreSQL。

跨 Stage 的长期限制同步见 [已知限制](acceptance/16-known-limitations.md)。
