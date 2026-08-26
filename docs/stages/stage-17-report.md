# Stage 17：服务端零钱回收与冻结式兑现闭环

状态：`IN_PROGRESS`

日期：2026-08-26

产品 Phase：V1 生产深度补齐

需求：REQ-005、REQ-007、REQ-009、REQ-021、REQ-025；限制：LIM-012

## 目标与非目标

用户可观察目标：孩子或家长先看到申请 Money、1:1 等回收比例、固定/比例手续费和预计线下到账；提交后由家长审批。批准只冻结 Money，其他消费不能穿透冻结；家长确认已在线下支付后才扣减余额并写流水。拒绝或撤销不扣钱并释放冻结。

范围内：版本化 WithdrawalRule、十分钟报价、申请/审批/支付/撤销状态机、可用/冻结余额、透明 CNY 明细、RBAC/对象权限、幂等、并发、H2/PostgreSQL 迁移和 API。范围外：银行/微信/支付宝接入、平台真实收款、自动打款、支付回调、税务与金融建议、儿童端复杂费率界面和真机 UI。

## 边界与不变量

- Money 是家庭教育账本；`netPayout` 只是家长线下约定，不代表 App 持有或转移真实资金。
- 默认规则为 1 Money = ¥1、手续费 0；家长可创建新版本，旧规则不可改写。
- `availableMoney = moneyBalance - reservedMoney` 且始终不小于 0；所有 Money 支出必须检查 available，而不是只检查总余额。
- `REQUESTED → APPROVED → PAID`；`REQUESTED → REJECTED/CANCELLED`；`APPROVED → CANCELLED`。非法跳转返回 409。
- APPROVED 同事务增加 reserved；PAID 同事务同时减少 balance/reserved、写一条 Money Ledger 和不可变的 gross/fee/net 线下明细；撤销同事务释放 reserved。
- 只有 PARENT 可创建规则、审批、确认 PAID 或撤销已批准申请；CHILD 只能操作自己的报价/申请/待审撤销。3–5 岁复杂费率仍由家长端托底。
- 每个写操作使用 `Idempotency-Key`；重放返回同一结果，载荷或目标不一致返回 409。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP17-1 | 进行中 | Spec、需求/限制回链与 V8 迁移 |
| WP17-2 | 未开始 | 领域报价、Store/Service 状态机和全局 Money 可用余额保护 |
| WP17-3 | 未开始 | REST API、OpenAPI、权限/幂等/费用明细 |
| WP17-4 | 未开始 | H2/PostgreSQL 16 并发与全量回归、证据和 Stage commit |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V17-01 | Java 领域 | 1:1、比例/固定费、舍入、净额非负向量 | 精确 BigDecimal 结果与非法规则拒绝 | Stage 17 evidence |
| V17-02 | H2 MockMvc | 报价→申请→批准→PAID；拒绝/撤销；401/403/404/409/幂等 | 状态、冻结、流水、透明明细正确 | Stage 17 evidence |
| V17-03 | H2/PG 并发 | 同余额并发批准/支出/支付 | 不超冻、不穿透 reserved、不重复扣账 | Stage 17 evidence |
| V17-04 | PostgreSQL 16 | Flyway V1–V8、`ddl-auto=validate`、全量测试 | 目标库迁移、约束和 API 通过 | Stage 17 evidence |
| V17-05 | 通用 | 文档链接、JSON、OpenAPI、diff/secret | 治理与契约一致 | Stage 17 evidence |

## 完成标准

- [ ] AC17-01 `PENDING`：版本化规则和默认 1:1、透明报价与十分钟过期实现。
- [ ] AC17-02 `PENDING`：REQUESTED/APPROVED/PAID/REJECTED/CANCELLED 状态机、RBAC 和对象权限实现。
- [ ] AC17-03 `PENDING`：reserved/available 约束覆盖所有 Money 支出，拒绝/撤销释放、PAID 原子扣账。
- [ ] AC17-04 `PENDING`：手续费/gross/net 不可变明细和 Money Ledger 可对账，重复请求不重复扣款。
- [ ] AC17-05 `PENDING`：H2/PostgreSQL 16、并发、权限、幂等、OpenAPI 和文档门禁通过。

## 安全检查、限制与交接

Stage 17 不实现真实支付。线下是否已给孩子现金只能由家长显式确认 `PAID`；系统不通过定位、消息或设备数据推断。Android 复杂界面不在本 Stage，3–5 岁儿童仍只看到“请家长一起”等简化路径。

2026-08-26 优先级说明：REQ-026–028 是用户当前测试反馈，Stage 18 暂时抢占代码实施。Stage 17 的 Spec 与待办继续保留，未完成内容不得视为取消或完成。
