# API 文档

基础路径：`/api/v1`。Stage 3 起采用服务端生产认证：除 bootstrap/login 外，所有业务接口必须携带 `Authorization: Bearer <opaque-token>`；数据库只保存 token SHA-256 哈希。家长 PIN 为 6 位且使用 BCrypt cost 12 保存，连续 5 次失败锁定 15 分钟。

## 认证与角色

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/bootstrap` | 公开 | 原子创建新家庭、首位家长、PIN 凭据和 12 小时家长会话 |
| POST | `/auth/login` | 公开 | 家长 PIN 登录；失败统一 401，锁定返回 429 |
| POST | `/auth/child-sessions` | PARENT | 为本家庭孩子创建 12 小时受限会话 |

不接受客户端自报 `role/familyId/childId` 作为授权事实。PARENT/CHILD、家庭和孩子范围来自服务端会话；跨家庭或无权对象与不存在对象统一返回 404。

## 家庭成长与任务奖励

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/families/{familyId}/children` | PARENT | 创建孩子并初始化 XP/Wallet |
| POST | `/families/{familyId}/children/{childId}/plans` | PARENT | 创建成长计划 |
| POST | `/families/{familyId}/plans/{planId}/goals` | PARENT | 创建成长目标 |
| POST | `/families/{familyId}/goals/{goalId}/tasks` | PARENT | 创建学习/成长任务 |
| POST | `/families/{familyId}/children/{childId}/tasks/{taskId}/completions` | CHILD 本人 | 提交 TaskCompletion，必须有 `Idempotency-Key` |
| POST | `/families/{familyId}/completions/{completionId}/review` | PARENT | 批准/拒绝并原子发放 XP/Coin/Money，必须有 `Idempotency-Key` |
| GET | `/families/{familyId}/children/{childId}/wallet` | PARENT/CHILD 本人 | 查询 Money/Coin 当前余额 |
| GET | `/families/{familyId}/children/{childId}/ledger?limit=50` | PARENT/CHILD 本人 | 查询最近不可变 Money/Coin 流水 |
| POST | `/families/{familyId}/children/{childId}/wallet/adjustments` | PARENT | 带原因和幂等键执行 Money/Coin 正负调账，禁止负余额 |
| GET | `/families/{familyId}/children/{childId}/wallet/reconciliation` | PARENT | 汇总 Ledger delta 并与 Wallet 对账 |
| POST | `/families/{familyId}/children/{childId}/gift-money` | PARENT | 幂等登记压岁钱并原子写入 Money 流水 |
| POST | `/families/{familyId}/exchange-rules` | PARENT | 创建新的双向比例、费用与单笔预算规则版本 |
| GET | `/families/{familyId}/exchange-rules/active` | PARENT/CHILD | 查询家庭当前适用规则 |
| POST | `/families/{familyId}/children/{childId}/exchange-previews` | PARENT/CHILD 本人 | 保存十分钟有效的本金、比例、费用、净额和教育声明快照 |
| POST | `/families/{familyId}/exchange-previews/{previewId}/confirm` | PARENT/CHILD 本人 | 以幂等键确认；规则未漂移时原子写 Money/Coin 双分录 |
| GET/POST | `/families/{familyId}/reward-products` | PARENT/CHILD / PARENT | 查询可用家庭奖励；家长配置 Coin 成本、库存和启用状态 |
| POST | `/families/{familyId}/children/{childId}/reward-orders` | PARENT/CHILD 本人 | 幂等创建奖励订单，不预扣 Coin |
| POST | `/families/{familyId}/reward-orders/{orderId}/review` | PARENT | 批准时才原子扣 Coin/库存；拒绝不扣款 |
| POST | `/families/{familyId}/reward-orders/{orderId}/cancel` | PARENT/CHILD 本人 | 取消 CREATED 订单，不扣款 |
| GET/POST | `/families/{familyId}/children/{childId}/saving[/transfers]` | PARENT/CHILD 本人 | 查询内部储蓄，或幂等转入/转出并保持总 Money 守恒 |
| GET/POST | `/families/{familyId}/children/{childId}/wishes` | PARENT/CHILD 本人 | 查询/创建愿望目标 |
| POST | `/families/{familyId}/wishes/{wishId}/allocation` | PARENT/CHILD 本人 | 显式分配真实储蓄，所有愿望总分配不得超过 Saving |
| GET/POST | `/families/{familyId}/funds` | PARENT/CHILD / PARENT | 查询/创建家庭纯模拟基金，固定返回风险教育声明 |
| POST | `/families/{familyId}/funds/{fundId}/nav` | PARENT | 写入可涨可跌的日期 NAV，限制单期 ±50% 且日期唯一 |
| POST | `/families/{familyId}/funds/{fundId}/fee-rules` | PARENT | 创建版本化买入/卖出费率 |
| POST | `/families/{familyId}/children/{childId}/funds/{fundId}/trade-previews` | PARENT/适龄 CHILD 本人 | 预览 NAV、费用、净额和份额；3–5 岁 CHILD 会话拒绝 |
| POST | `/families/{familyId}/fund-trade-previews/{previewId}/confirm` | PARENT/适龄 CHILD 本人 | 幂等确认，NAV/规则漂移返回 409 |
| GET | `/families/{familyId}/children/{childId}/funds/{fundId}/position` | PARENT/CHILD 本人 | 返回份额、加权成本、市值和已实现/未实现损益 |
| GET | `/families/{familyId}/children/{childId}/sync` | PARENT/CHILD 本人 | Android 聚合同步任务/最新 Completion、钱包和今日审核摘要 |
| GET/PUT | `/families/{familyId}/children/{childId}/usage-policy` | PARENT/CHILD 本人 / PARENT | 查询或配置家庭时区、每日和单次 App 内时长上限；缺省为 Asia/Shanghai 20/10 分钟 |
| POST | `/families/{familyId}/children/{childId}/usage-events` | PARENT/CHILD 本人 | 幂等记录本 App 的活跃/学习分钟；只接受最近 31 天且不超过未来 5 分钟的事件 |
| GET | `/families/{familyId}/children/{childId}/reports/today` | PARENT/CHILD 本人 | 按家庭时区返回本人适龄使用、任务、待审核和钱包摘要 |
| GET | `/families/{familyId}/children/{childId}/reports/monthly` | PARENT | 从 Usage/Completion/Ledger/Saving/Fund 事实表聚合月度成长和财商报告 |

批准审核会锁定 Completion 与 Wallet；Coin/Money 流水先追加，再在同一事务更新余额和 Completion。重复提交返回首次结果；重复/冲突审核返回 409，不能重复发奖。XP 的奖励事实保存在 Completion 快照并更新 `child_progress`。

月度报告不保存第二份余额：Money/Coin 收支来自不可变 Ledger，压岁钱、兑换费、储蓄和模拟基金分别来自业务事实表；`walletLedgerBalanced` 直接比较 Wallet 与全量流水。CHILD 会话不能读取月度财务报告，也不能读取其他孩子的今日摘要。

## 响应与错误

成功与失败统一为 `{data,error,traceId}`。当前错误码：

| HTTP | code | 含义 |
| ---: | --- | --- |
| 400 | `VALIDATION_FAILED` | Bean Validation、缺失/非法幂等键或请求不满足业务前置 |
| 401 | `AUTHENTICATION_REQUIRED` / `AUTHENTICATION_FAILED` | 无有效会话或 PIN 不匹配 |
| 403 | `FORBIDDEN` | 已认证角色不允许该操作 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在或不在会话对象范围内 |
| 409 | `CONFLICT` | 状态、并发或幂等键冲突 |
| 429 | `PIN_LOCKED` | PIN 失败达到阈值，暂时锁定 |

字段、校验、Bearer scheme 和完整响应以 [OpenAPI 3.1](../openapi.yaml) 为机器契约。
