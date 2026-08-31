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
| 17 | V1 深度 | 服务端零钱回收与冻结式兑现闭环 | `COMPLETED` |
| 18 | V1 Android | 双视角排版、奖励浏览与教学视频任务 | `BLOCKED` |
| 19 | 发布可靠性 | GitHub Release 下载 0% 真机修复 | `COMPLETED` |
| 20 | 全面教学基座 | 学段底座、家长配置与分层体验路由 | `BLOCKED` |
| 21 | 全面教学核心 | 共用课程、活动与学习证据引擎 | `BLOCKED` |
| 22 | 全面教学幼儿园深度 | 幼儿园故事舞台与亲子现实活动 | `BLOCKED` |
| 23 | 全面教学小学深度 | 小学探索手册 | `BLOCKED` |
| 24 | 全面教学初中深度 | 初中学科实验台 | `BLOCKED` |
| 25 | 全面教学高中深度 | 高中自主学习室 | `BLOCKED` |
| 26 | 全面教学收口 | 连续性、隐私、防沉迷与发布总验收 | `BLOCKED` |
| 27 | V1 成长深化 | 成长计划、里程碑与家庭成长档案 | `BLOCKED` |
| 28 | V1 奖励治理 | 奖励预算、兑换额度与履约闭环 | `NOT_STARTED` |
| 29 | V1 家庭协作 | 多家长/多孩子、配对与最小通知 | `NOT_STARTED` |
| 30 | V1 可靠同步 | Room/加密迁移、增量同步与冲突传播 | `NOT_STARTED` |
| 31 | V1 虚拟经济深化 | 储蓄利息、模拟市场、持有期费用与统一实验室 | `NOT_STARTED` |
| 32 | 工程治理 | Android 工具链与最终全量治理验收 | `NOT_STARTED` |

Stage 11 是用户直接提出的发布能力，编号不代表业务交付顺序；它不改变 Stage 2 的 Android 运行态阻塞，也不代表 Stage 3–10 已启动或完成。

Stage 12 是用户直接提出的 P0 长期治理要求，只调整文档结构和 Agent 执行协议，不改变任何运行时 Stage 的完成状态。

Stage 13 按用户最新优先级已交付 Stage 2–11 的 Android 本地基础体验宽度、五区前端和 0.2.0 debug APK；真机运行态仍阻塞。深度服务端定制后移，不改变 Stage 3–10 完整生产能力仍待建设的事实。

Stage 14 将 GitHub Release 热更新提升为 P0，负责公开仓库、分 Stage Git 历史、稳定签名 Release 和真实远端更新契约；它延续 Stage 11，但不以仓库创建替代真机覆盖升级。

REQ-020/REQ-022 已连续推进 Stage 3–10。Stage 3–7、9 已在 PostgreSQL 通过；Stage 8 可离线实现完成但真机阻塞，Stage 10 的部署、恢复、自动化和 v0.3.0 Release 已通过，仅最终设备/目标服务验收阻塞。

Stage 15 来自 REQ-021 的最高行为准则，优先于普通功能迭代。儿童端可离线代码/自动化已收敛，仅真实平板验收阻塞；Stage 5 因而恢复推进，儿童门禁继续约束其后所有界面和功能。

Stage 16 来自 REQ-023，延续儿童最佳利益与 GitHub Release P0 约束；先完成低刺激 Launcher 品牌、Android 全形态资源和稳定 v0.3.1，真机桌面视觉与覆盖升级单独保留阻塞。

Stage 17 来自 REQ-025，优先解除 LIM-012；补齐 WithdrawalRule、透明报价、批准冻结、PAID 扣账和所有 Money 支出的 available 保护，不引入真实支付。

Stage 18 来自用户当前测试反馈 REQ-026–028，暂时抢占 Stage 17 的代码实施；在不增加儿童一级入口、不绕过 PIN/审核和不引入第三方视频风险的前提下完成 Android 交互与 v0.3.2 更新交付。

Stage 18 的可离线实现和公开 v0.3.2 已完成；只缺真实平板上的按钮排版、视频实际播放、兴趣/观看进度重启保留和 v0.3.1→v0.3.2 应用内覆盖升级，因此保持 `BLOCKED`。Stage 17 恢复为 Agent 下一主线。

Stage 19 来自 REQ-029/BUG-004：用户真机证明 v0.3.1 的直接流式下载仍会卡在首字节 0%。本 Stage 再次抢占 Stage 17，改用系统下载调度、明确阶段和官方 Asset API 备用入口，校验与安装安全边界不降级。

Stage 19 可离线实现和 v0.3.3→v0.3.4 两版测试链已完成；用户真机确认可正常更新，BUG-004 已关闭。旧数据逐项盘点仍归 Stage 11/14，随后转入并完成 Stage 17。

Stage 17 随后完成 V8、版本化零钱回收规则/报价、批准冻结、PAID 扣账、拒绝/撤销释放和全部既有 Money 支出 available 保护；H2/PostgreSQL 16.15 全量与并发门禁通过，LIM-012 解除。

REQ-030/031 把全面教学确定为下一产品主线，并按幼儿园、小学、初中、高中重排。Stage 20–25 的可离线范围均已完成，因目标平板/服务验收保持 `BLOCKED`。REQ-050 授权连续交付后，主线已进入 Stage 26 的跨学段连续性、数据权利、完整防沉迷与发布总验收。

REQ-051 的全量完成性审计确认 P-01–P-21 仍有成长档案、奖励治理、家庭协作、可靠同步、虚拟经济和工具链深化。依赖顺序固定为 Stage 27→32；设备/第三方许可阻塞不暂停可离线工程。
