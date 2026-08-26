# Stage 路线图

| Stage | Phase | 主题 | 状态 |
| ---: | ---: | --- | --- |
| 1 | 0 | 项目立项与总体设计 | `COMPLETED` |
| 2 | 1 | 工程骨架、Family/Parent/Child/Growth 基础 | `BLOCKED` |
| 3 | 2 | 生产认证、TaskCompletion、家长审核与奖励闭环 | `COMPLETED` |
| 4 | 3 | Wallet 与 Ledger | `COMPLETED` |
| 5 | 4 | GiftMoney、ExchangeRule 与 Coin | `COMPLETED` |
| 6 | 5 | RewardShop、Saving 与 Wish | `COMPLETED` |
| 7 | 6 | VirtualFund、NAV、订单、持仓与费用 | `COMPLETED` |
| 8 | 7 | Android 家长端/孩子端核心页面 | `BLOCKED` |
| 9 | 8 | 跨域业务闭环与报告 | `COMPLETED` |
| 10 | 9 | APK 构建、安装与 V1 总验收 | `BLOCKED` |
| 11 | 计划外 | GitHub Release APK 热更新与当前版本交付 | `BLOCKED` |
| 12 | 计划外 | 参考仓库文档治理与 Agent 启动协议对齐 | `COMPLETED` |
| 13 | 计划外 | Stage 2–11 基础体验宽度与 Android 前端完善 | `BLOCKED` |
| 14 | 计划外 | 公开 GitHub 仓库、分阶段提交与 Release 更新链 | `BLOCKED` |
| 15 | 计划外 | 3 岁起儿童端发展适龄改造 | `BLOCKED` |
| 16 | 计划外 | 儿童舒适品牌图标与 v0.3.1 交付 | `BLOCKED` |

Stage 11 是用户直接提出的发布能力，编号不代表业务交付顺序；它不改变 Stage 2 的 Android 运行态阻塞，也不代表 Stage 3–10 已启动或完成。

Stage 12 是用户直接提出的 P0 长期治理要求，只调整文档结构和 Agent 执行协议，不改变任何运行时 Stage 的完成状态。

Stage 13 按用户最新优先级已交付 Stage 2–11 的 Android 本地基础体验宽度、五区前端和 0.2.0 debug APK；真机运行态仍阻塞。深度服务端定制后移，不改变 Stage 3–10 完整生产能力仍待建设的事实。

Stage 14 将 GitHub Release 热更新提升为 P0，负责公开仓库、分 Stage Git 历史、稳定签名 Release 和真实远端更新契约；它延续 Stage 11，但不以仓库创建替代真机覆盖升级。

REQ-020/REQ-022 已连续推进 Stage 3–10。Stage 3–7、9 已在 PostgreSQL 通过；Stage 8 可离线实现完成但真机阻塞，Stage 10 的部署、恢复、自动化和 v0.3.0 Release 已通过，仅最终设备/目标服务验收阻塞。

Stage 15 来自 REQ-021 的最高行为准则，优先于普通功能迭代。儿童端可离线代码/自动化已收敛，仅真实平板验收阻塞；Stage 5 因而恢复推进，儿童门禁继续约束其后所有界面和功能。

Stage 16 来自 REQ-023，延续儿童最佳利益与 GitHub Release P0 约束；先完成低刺激 Launcher 品牌、Android 全形态资源和稳定 v0.3.1，真机桌面视觉与覆盖升级单独保留阻塞。
