# 当前阶段

最后更新：2026-08-26。

## 当前主线

| Stage | Phase | 主题 | 状态 | 下一动作 |
| ---: | ---: | --- | --- | --- |
| 1 | 0 | 项目立项与总体设计 | `COMPLETED` | 已进入工程迭代 |
| 2 | 1 | 工程骨架与 Family / Growth 基础 | `BLOCKED` | Emulator 同版本官方重装无效；需授权重装共享 API 34 system image，或连接 Android 真机/平板 |
| 3 | 2 | 生产认证、TaskCompletion、审核与奖励 | `COMPLETED` | PostgreSQL 16.15 完整 API 与权限门禁通过 |
| 4 | 3 | Wallet/Ledger 生产闭环 | `COMPLETED` | 调账、幂等、非负、对账和 PostgreSQL 并发门禁通过 |
| 5 | 4 | GiftMoney、兑换规则与 Coin | `COMPLETED` | H2/PostgreSQL 双向兑换、透明费用和账本门禁通过 |
| 6 | 5 | RewardShop、Saving 与 Wish | `COMPLETED` | H2/PostgreSQL 订单、储蓄守恒、愿望和并发门禁通过 |
| 7 | 6 | 纯模拟基金 | `COMPLETED` | H2/PostgreSQL NAV、费用、并发、持仓与 P&L 门禁通过 |
| 8 | 7 | Android 生产接入 | `BLOCKED` | 代码/JVM/lint/build 通过；需平板回放真实登录同步与无障碍 |
| 9 | 8 | 跨域闭环与报告 | `COMPLETED` | H2/PostgreSQL 使用事件、事实报告、权限和 Android 重试门禁通过 |
| 10 | 9 | V1 发布、部署与总验收 | `BLOCKED` | 部署/恢复/v0.3.0 Release 已过；需平板完成服务 E2E、无障碍和覆盖升级 |
| 11 | 计划外 | GitHub Release APK 热更新与当前版本交付 | `BLOCKED` | v0.2.0/0.2.1/0.2.2 同签名 Release 已完成；只需 Android 设备回放 App 内覆盖升级 |
| 12 | 计划外 | 参考仓库文档治理与 Agent 启动协议对齐 | `COMPLETED` | 后续每次对话持续执行 AGENTS Step 1–6 和需求永久登记 |
| 13 | 计划外 | Stage 2–11 基础体验宽度与 Android 前端完善 | `BLOCKED` | 代码、自动化和 0.2.0 APK 已交付；需 Android 真机/平板回放交互、计时与重启持久化 |
| 14 | 计划外 | 公开 GitHub 仓库、分阶段提交与 Release 更新链 | `BLOCKED` | 三版同签名 Release 与 digest 已验证；只需 Android 设备回放覆盖升级/数据保留 |
| 15 | 计划外 | 3 岁起儿童端发展适龄改造 | `BLOCKED` | 代码/自动化/稳定签名包通过；需平板触控、TalkBack、字体放大和限时退出回放 |
| 16 | 计划外 | 儿童舒适品牌图标与 v0.3.1 交付 | `BLOCKED` | 更新修复、图标和 v0.3.1 Release 已过；需平板验证桌面蒙版与实际更新 |

Stage 2 的代码、PostgreSQL 和自动化门禁已交付，debug APK 已生成；Android 安装启动因本机 Emulator 37.1.11 在 ADB 可用前退出而阻塞。未完成运行态验收前不得把 APK 描述为已安装可用。

Stage 11 的实现、自动化门禁和本地 APK 已交付；Stage 14/15 已完成公开仓库、稳定签名、三版 Release 与下载复验。只缺 Android 设备上的 App 内下载、系统确认和数据保留，不能标记完成。

Stage 12 已将 docs 根入口、一级分类、BLOCKERS/TODO/Stage 职责、OpenAPI、文档门禁和每次对话六步协议与参考仓库对齐。该长期标准由 AGENTS 和 REQ-014 持续约束后续任务。

Stage 13 的可离线工程工作已收口：Android 本地基础体验覆盖任务、奖励、钱包、成长、防沉迷、报告和更新，五区响应式前端与 0.2.0 APK 构建通过。当前只缺真机/平板运行态和重启持久化回放，因此保持 `BLOCKED`；服务端生产闭环仍由 Stage 3–10 后续深化。

Stage 14 已完成公开仓库、分阶段提交、main 推送、稳定签名备份/Secrets 和 Android 更新源绑定；Stage 15 又发布 v0.2.2。latest API、digest、版本和三版同证书下载复验均通过。只缺 Android 真机/平板覆盖升级与数据保留，因此保持 `BLOCKED`。

REQ-020/REQ-022 已推进 Stage 3–10。Stage 3–7、9 后端已完成；Stage 8/10 可离线门禁、部署恢复和 v0.3.0 Release 已过，当前等待真机/目标 HTTPS 服务回放。

REQ-021 将“儿童最佳利益、3 岁起、极简低刺激、无操纵设计”提升为最高产品行为准则。该门禁已进入 0.3.0；真实平板验收仍阻塞。REQ-023 已启动 Stage 16，以相同准则完成儿童舒适图标和 v0.3.1 更新交付。

Stage 16 的 BUG-002 弱网下载修复、全形态 Launcher 图标和公开 v0.3.1 已完成。旧 v0.3.0 需要手动安装 v0.3.1 才能获得修复；无真实平板，应用内下载、桌面蒙版和覆盖升级仍不能标记通过。

## 关键决策快照

- 一个 APK，登录后按 PARENT/CHILD 切换，家长模式受 PIN 保护。
- Android 平板横屏是首要 UI 基线；学习任务、App 内防沉迷和今日使用统计属于 V1 一级范围。
- Spring Boot 模块化单体 + Android 原生 Compose；HTTP/JSON 通信，家庭局域网优先。
- V1 推荐 PostgreSQL 作为正式/验收事实库，H2 仅用于隔离测试，不以 SQLite 替代服务端事务语义。
- Wallet/Ledger 是金额与 Coin 的唯一事实链；虚拟投资不接真实行情与支付。
- 默认零钱回收比例 1:1；家长可配置透明手续费，但平台不作为真实金融中间商收款。
- 所有用户需求与建议必须追加登记到 `docs/requirements/requirement-ledger.md`，按 P0–P3 和固定状态长期追踪，不依赖会话记忆。
- 儿童端从 3 岁起，3–5 岁默认亲子共用；儿童最佳利益高于留存、时长、奖励和金融功能完整度，一级导航最多三个且禁止操纵性参与机制。
