# 当前阶段

最后更新：2026-08-25。

## 当前主线

| Stage | Phase | 主题 | 状态 | 下一动作 |
| ---: | ---: | --- | --- | --- |
| 1 | 0 | 项目立项与总体设计 | `COMPLETED` | 已进入工程迭代 |
| 2 | 1 | 工程骨架与 Family / Growth 基础 | `BLOCKED` | Emulator 同版本官方重装无效；需授权重装共享 API 34 system image，或连接 Android 真机/平板 |
| 3 | 2 | 生产认证、TaskCompletion、审核与奖励 | `IN_PROGRESS` | 实现认证/RBAC、Completion、三奖励和最小账本闭环 |
| 4–7 | 3–6 | Wallet/Ledger 至纯模拟基金生产后端 | `NOT_STARTED` | Spec 已建立；Stage 3 通过后顺序推进 |
| 11 | 计划外 | GitHub Release APK 热更新与当前版本交付 | `BLOCKED` | v0.2.0/0.2.1 两版同签名 Release 已完成；只需 Android 设备回放 App 内覆盖升级 |
| 12 | 计划外 | 参考仓库文档治理与 Agent 启动协议对齐 | `COMPLETED` | 后续每次对话持续执行 AGENTS Step 1–6 和需求永久登记 |
| 13 | 计划外 | Stage 2–11 基础体验宽度与 Android 前端完善 | `BLOCKED` | 代码、自动化和 0.2.0 APK 已交付；需 Android 真机/平板回放交互、计时与重启持久化 |
| 14 | 计划外 | 公开 GitHub 仓库、分阶段提交与 Release 更新链 | `BLOCKED` | v0.2.0/0.2.1 同签名 Release 与 digest 已验证；只需 Android 设备回放覆盖升级/数据保留 |

Stage 2 的代码、PostgreSQL 和自动化门禁已交付，debug APK 已生成；Android 安装启动因本机 Emulator 37.1.11 在 ADB 可用前退出而阻塞。未完成运行态验收前不得把 APK 描述为已安装可用。

Stage 11 的实现、自动化门禁和本地 APK 已交付；Stage 14 已完成公开仓库、稳定签名、两版 Release 与下载复验。只缺 Android 设备上的 App 内下载、系统确认和数据保留，不能标记完成。

Stage 12 已将 docs 根入口、一级分类、BLOCKERS/TODO/Stage 职责、OpenAPI、文档门禁和每次对话六步协议与参考仓库对齐。该长期标准由 AGENTS 和 REQ-014 持续约束后续任务。

Stage 13 的可离线工程工作已收口：Android 本地基础体验覆盖任务、奖励、钱包、成长、防沉迷、报告和更新，五区响应式前端与 0.2.0 APK 构建通过。当前只缺真机/平板运行态和重启持久化回放，因此保持 `BLOCKED`；服务端生产闭环仍由 Stage 3–10 后续深化。

Stage 14 已完成公开仓库、分阶段提交、main 推送、稳定签名备份/Secrets、Android 更新源绑定和 v0.2.0→v0.2.1 两版公开 Release；latest API、digest、版本和同证书下载复验均通过。只缺 Android 真机/平板覆盖升级与数据保留，因此保持 `BLOCKED`。

REQ-020 已恢复 Stage 3–7 生产后端闭环。Stage 3 当前建设生产认证、服务端 RBAC/对象权限、TaskCompletion 审核以及 XP/Coin/Money 原子奖励；Stage 4–7 的 Spec 已建立但不跨越账本前置条件虚假标记启动。

## 关键决策快照

- 一个 APK，登录后按 PARENT/CHILD 切换，家长模式受 PIN 保护。
- Android 平板横屏是首要 UI 基线；学习任务、App 内防沉迷和今日使用统计属于 V1 一级范围。
- Spring Boot 模块化单体 + Android 原生 Compose；HTTP/JSON 通信，家庭局域网优先。
- V1 推荐 PostgreSQL 作为正式/验收事实库，H2 仅用于隔离测试，不以 SQLite 替代服务端事务语义。
- Wallet/Ledger 是金额与 Coin 的唯一事实链；虚拟投资不接真实行情与支付。
- 默认零钱回收比例 1:1；家长可配置透明手续费，但平台不作为真实金融中间商收款。
- 所有用户需求与建议必须追加登记到 `docs/requirements/requirement-ledger.md`，按 P0–P3 和固定状态长期追踪，不依赖会话记忆。
