# Stage 6：RewardShop、Saving 与 Wish 闭环

状态：`COMPLETED`

产品 Phase：5　需求：REQ-001、REQ-005、REQ-020

## 目标与非目标

用户可观察目标：家长配置奖励商品，孩子提交兑换并由家长批准扣 Coin；孩子把 Money 转入/转出储蓄子账户并跟踪愿望目标，家庭净资产不凭空变化。

范围内：RewardProduct/Order 状态机、批准扣 Coin、SavingAccount/Transaction、愿望目标与进度、幂等和权限。范围外：真实商品履约物流、真实利息、真实储蓄账户或支付。

## 边界与不变量

- 奖励订单 `CREATED → APPROVED | REJECTED | CANCELED`，批准时才原子扣 Coin；重复批准不重复扣。
- 储蓄转入/转出使用同组账本分录或明确子账户投影，Money 总资产守恒；余额不得为负。
- 商品配置/批准仅 PARENT；CHILD 只能操作本人订单、储蓄和愿望。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP6-1 | 已完成 | 奖励商品与订单状态机 |
| WP6-2 | 已完成 | 批准扣 Coin、库存/启用校验与幂等 |
| WP6-3 | 已完成 | 储蓄账户转移、愿望目标和进度 |
| WP6-4 | 已完成 | 权限、并发、守恒和 PostgreSQL 测试 |

## 完成标准

- [x] AC6-01 PASS：批准只追加一次 Coin Ledger；拒绝和取消不扣 Coin。
- [x] AC6-02 PASS：禁用商品、余额不足、不同幂等键重复审核返回 409；家庭/孩子对象权限复用服务端会话门禁。
- [x] AC6-03 PASS：储蓄转入/转出同时更新 Wallet 与 Saving，数据库和领域双重校验总 Money 守恒并保留交易/流水。
- [x] AC6-04 PASS：愿望进度仅由 target 与显式 allocatedAmount 计算，总分配不得超过真实 Saving balance。
- [x] AC6-05 PASS：Flyway V1–V5、H2/PostgreSQL 16.15 全量回归和并发双审核门禁通过。

## 安全检查、已知限制与交接

RewardShop 是家庭约定奖励，不代表真实电商；Saving 只属于内部教育账本，不计真实利息。

实现入口：`Stage6Models`、`Stage6Service`、`JdbcStage6Store`、`Stage6Controller` 和 Flyway `V5__stage6_reward_saving_wish.sql`。证据：[Stage 6 acceptance](../evidence/stage-6/acceptance.json)。
