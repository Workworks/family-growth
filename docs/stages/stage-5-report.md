# Stage 5：GiftMoney、兑换规则与 Coin 闭环

状态：`COMPLETED`

产品 Phase：4　需求：REQ-005、REQ-007、REQ-009、REQ-020

## 目标与非目标

用户可观察目标：家长登记压岁钱后 Money 入账；家长配置版本化双向兑换规则；孩子先看到比例、费用和预计到账，再以幂等确认完成 Money/Coin 兑换。

范围内：GiftMoney、ExchangeRule、兑换预览/确认、规则快照、透明费用、预算护栏、同组双资产流水。范围外：真实人民币转账、平台收费、动态汇率和多币种。

## 边界与不变量

- 默认零钱回收/线下单位比例为 1:1；Money↔Coin 的买入/回兑比例可不同，价差必须明确展示。
- 预览保存规则版本、输入、费用和预计到账；确认时版本或金额变化必须 409 并要求重预览。
- GiftMoney 仅 PARENT 登记；兑换允许 CHILD 本人或 PARENT，均执行对象权限与余额检查。
- 两种资产变化同一事务、同一 ledger group，任何一侧失败整体回滚。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP5-1 | 已完成 | GiftMoney 入账与幂等审计 |
| WP5-2 | 已完成 | 版本化 ExchangeRule 与家长配置 |
| WP5-3 | 已完成 | 双向兑换预览/确认和费用快照 |
| WP5-4 | 已完成 | 精度、价差、规则漂移、权限和 PostgreSQL 测试 |

## 完成标准

- [x] AC5-01 PASS：¥100 GiftMoney 重放只保留一条业务记录和一条 Money Ledger，关联 group。
- [x] AC5-02 PASS：1 Money=10 Coin 与 12 Coin=1 Money 固定向量在领域/API 测试通过。
- [x] AC5-03 PASS：预览快照返回本金、适用比例、费率、费用、净源金额、预计到账和教育声明。
- [x] AC5-04 PASS：规则漂移、幂等键复用、余额不足和 CHILD 配规则均返回 403/409 且事务回滚；家庭/孩子对象权限复用 Stage 3 门禁。
- [x] AC5-05 PASS：Flyway V1–V4、Hibernate validate、完整 API 与账本回归在 H2/PostgreSQL 16.15 通过。

## 安全检查、已知限制与交接

全部金额是家庭内部教育账本；不得接支付、银行或平台真实收费。预算策略首版采用明确上限与拒绝，不静默把 Money 奖励改成其他资产。

实现入口：`Stage5Models`、`Stage5Service`、`JdbcStage5Store`、`Stage5Controller` 与 Flyway `V4__stage5_gift_exchange.sql`。验收证据：[Stage 5 acceptance](../evidence/stage-5/acceptance.json)。Stage 5 不提供真实提现或真实资金流。
