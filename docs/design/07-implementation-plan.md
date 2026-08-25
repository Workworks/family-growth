# 07 实施计划

## 分阶段策略

| Stage / Phase | 交付 | 关键门禁 |
| --- | --- | --- |
| 1 / 0 | 立项、八份设计、Spec、路线图 | 链接、结构、核心模型评审 |
| 2 / 1 | Maven 多模块、Spring Boot、Flyway、Android 平板骨架；Family/Parent/Child/Plan/Goal/学习与成长 Task | 构建、迁移、横竖屏基线、权限反向、debug APK |
| 3 / 2 | Completion、审核、XP/Coin/Money 奖励 | 幂等审核与三奖励闭环 |
| 4 / 3 | Wallet/Ledger | 并发、精度、不可负、对账、调账审计 |
| 5 / 4 | GiftMoney、双向兑换、预算 | 价差与透明费用测试 |
| 6 / 5 | RewardShop、Saving、Wish | 订单状态、储蓄转移、愿望进度 |
| 7 / 6 | VirtualFund、NAV、买卖、持仓、费用 | 固定向量公式、涨跌、FIFO/费率、Ledger |
| 8 / 7 | Android 家长/孩子端核心页面、防沉迷与使用统计 | Compose UI、平板横屏、权限栈、时长拦截、费用预览、可访问性 |
| 9 / 8 | 全链路、今日使用/成长/财商报告、0–2 岁与里程碑完善 | E2E、局域网、断网/重试/幂等 |
| 10 / 9 | APK、安装、数据保留与 V1 验收 | 真机安装、签名、完整验收矩阵 |

## Stage 2 前置决策

1. 确认正式数据库（推荐 PostgreSQL）与部署宿主。
2. 确认认证形态、孩子登录/切换路径与 PIN 锁定策略。
3. 冻结 Money/Coin 精度、账本写入契约、储蓄账户语义和提现冻结策略。
4. 冻结包名、应用名、最低/目标 Android SDK 和后端坐标。
5. 冻结 App 内防沉迷的默认每日时长、连续使用时长、休息规则和家长临时解锁有效期。

## 实施原则

每个 Stage 先写同号 Spec，再实现最小闭环。数据库、API、后端服务、Android 和测试必须按同一验收 ID 推进。不得先生成全部 UI 或在账本前实现基金交易。每阶段完成后更新 OpenAPI、文档、证据与下一阶段交接。
