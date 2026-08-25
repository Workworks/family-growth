# Stage 7：VirtualFund、NAV、订单、持仓与费用闭环

状态：`COMPLETED`

产品 Phase：6　需求：REQ-008、REQ-009、REQ-020

## 目标与非目标

用户可观察目标：家长创建纯模拟基金和版本化费用规则；NAV 可涨可跌；孩子先预览费用再确认买卖，持仓、市值、损益、Money 和流水保持一致。

范围内：VirtualFund/NAV/FeeRule、买卖预览与确认、持仓、买入费/卖出费、已实现/未实现损益、幂等和并发。范围外：真实基金代码、行情、证券账户、支付、收益承诺与投资建议。

## 边界与不变量

- NAV `NUMERIC(19,6)`、份额 `NUMERIC(19,8)`；价格和费率计算使用 BigDecimal 与固定舍入。
- NAV 引擎必须允许正负 shock，单期护栏且 `(fund_id, nav_date)` 唯一。
- 预览保存 NAV/费用规则版本和过期时间；确认时漂移或过期返回 409。
- 买入扣 Money、增份额；卖出减份额、按净额增 Money；费用和损益透明且同事务记账。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP7-1 | 已完成 | 模拟基金、NAV 与版本化费率 |
| WP7-2 | 已完成 | 买入/卖出预览、确认与持仓 |
| WP7-3 | 已完成 | 市值、损益、费用和 Ledger 对账 |
| WP7-4 | 已完成 | 固定向量、涨跌、漂移、并发、权限和 PostgreSQL 测试 |

## 完成标准

- [x] AC7-01 PASS：NAV 1.00、投入 20.00、买入费 5% 的固定向量精确得到 1.00/19.00/19.00000000。
- [x] AC7-02 PASS：+10% 与 -10% NAV 均写入；正值、单期 ±50% 和 fund/date 唯一约束生效。
- [x] AC7-03 PASS：买卖在同一事务锁 Wallet/Position，写费用/损益订单和 Money Ledger 后更新余额/份额。
- [x] AC7-04 PASS：NAV/费率漂移、不同键重放、超卖、重复日期和幼儿会话交易拒绝且无余额副作用；十分钟过期由服务端校验。
- [x] AC7-05 PASS：Flyway V1–V6、H2/PostgreSQL 16.15 全量 API、并发确认、持仓/账本和 P&L 门禁通过。

## 安全检查、已知限制与交接

所有名称和价格均为本系统虚构；API 与界面必须显示“纯模拟、可能涨跌、非投资建议”。V1 可先采用加权成本；若实现 FIFO 批次，须在迁移与验收向量中明确。

实现采用含买入费的加权成本。3–5 岁 CHILD 会话被服务端拒绝交易，家长可共同操作。实现入口：`Stage7Models`、`Stage7Service`、`JdbcStage7Store`、`Stage7Controller` 与 Flyway `V6__stage7_virtual_fund.sql`。证据：[Stage 7 acceptance](../evidence/stage-7/acceptance.json)。
