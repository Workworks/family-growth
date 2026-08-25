# Stage 4：Wallet 与 Ledger 生产闭环

状态：`COMPLETED`

产品 Phase：3　需求：REQ-005、REQ-009、REQ-020

## 目标与非目标

用户可观察目标：家长能查询孩子 Money/Coin 钱包和分页流水，带原因执行调账；系统能证明余额与流水一致，在并发和重放下不超扣、不重复记账。

范围内：钱包版本、不可变流水、幂等操作、家长调账、余额/流水查询、对账、乐观并发、统一金额舍入与审计。范围外：真实支付、信用余额、流水修改/删除、跨币种结算。

## 边界与不变量

- Money 使用 `BigDecimal`/`NUMERIC(19,2)`、`HALF_UP`；Coin 为整数。
- 每次 Money/Coin 变化必须同事务追加 LedgerEntry；历史流水不得更新或删除。
- 调账仅 PARENT，可见原因和操作者；跨家庭对象返回 404。
- 余额非负；并发更新采用版本或原子条件更新；同幂等键返回原结果或冲突，不重复执行。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP4-1 | 完成 | Wallet/Ledger 领域运算、entry type 与规则 |
| WP4-2 | 完成 | 原子余额更新、幂等持久化和不可变约束 |
| WP4-3 | 完成 | 查询、家长调账、对账 API |
| WP4-4 | 完成 | 精度、负余额、并发、重放和 PostgreSQL 门禁 |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V4-01 | JVM | 金额舍入与 before/delta/after 向量 | 精确且守恒 | domain tests |
| V4-02 | Spring | 调账、查询、CHILD/跨家庭反向 | 正向成功，反向无副作用 | integration tests |
| V4-03 | PostgreSQL 16 | 并发扣款、相同幂等键、对账 | 不超扣、不重复、差额为零 | Stage 4 evidence |

## 完成标准

- [x] AC4-01 `PASS`：Money/Coin 钱包与流水查询按家庭/孩子授权访问。
- [x] AC4-02 `PASS`：家长调账强制原因/幂等键，先写流水再更新余额，单事务提交。
- [x] AC4-03 `PASS`：PostgreSQL 同时两笔超额组合扣款仅一笔成功，最终余额 3.00 且不为负。
- [x] AC4-04 `PASS`：Ledger delta 与 Wallet 对账为零差异；无流水修改 API，V2/V3 约束保护算术、非负和幂等。
- [x] AC4-05 `PASS`：H2 隔离测试与 PostgreSQL 16.15 全门禁通过，见 [证据](../evidence/stage-4/acceptance.json)。

## 安全检查、已知限制与交接

本 Stage 复用 Stage 3 服务端认证。纠错只能生成反向/新调账流水，不提供历史流水编辑接口。
