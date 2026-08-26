# Stage 9：跨域闭环、使用统计与家庭报告

状态：`COMPLETED`

产品 Phase：8　需求：REQ-001、REQ-002、REQ-003、REQ-006、REQ-022

## 目标与非目标

用户可观察目标：学习任务、奖励、钱包、储蓄、模拟基金和 App 内使用时长汇总为可对账的今日/月度家庭报告；离线重试不重复写入。

范围内：UsagePolicy/UsageEvent、今日摘要、月度成长/财商报告、Ledger/订单/持仓对账、同步游标和跨域 API E2E。范围外：系统级其他 App 使用数据、监控儿童通信、AI 投资建议和真实金融报表。

## 边界与不变量

- 只统计本 App；使用事件按家庭/孩子/幂等键去重。
- 财务报告由不可变流水和订单聚合，不保存可漂移的手工余额。
- CHILD 只能读取本人适龄摘要；完整财务报告仅 PARENT。
- 报告时区由家庭配置，数据库时间保持 UTC。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP9-1 | 已完成 | UsagePolicy、UsageEvent 幂等记录与家庭时区今日汇总 |
| WP9-2 | 已完成 | 从 Usage/Completion/Ledger/Saving/Fund 事实表聚合月度报告和钱包对账 |
| WP9-3 | 已完成 | Android 保留最后成功快照，使用事件失败后复用幂等键重试，401 清理会话 |
| WP9-4 | 已完成 | H2/PostgreSQL 16.15 跨域 E2E、权限反向和全量回归 |

## 数据、API、Android 与文档变化

- Flyway V7 新增 `usage_policy`、`usage_event`、家庭/幂等唯一约束和孩子时间索引；数据库时间保持 UTC，汇总边界按策略 `ZoneId` 计算。
- 新增策略查询/家长配置、App 内使用事件、今日摘要和家长月报 API；未配置时使用儿童安全默认 `Asia/Shanghai` 每日 20 分钟、单次 10 分钟。
- 月报不保存可漂移副本：Money/Coin 收支来自 Ledger，礼金、兑换费用、储蓄、基金订单/持仓来自各自事实表，并返回 Wallet/Ledger 对账结果。
- CHILD 只能查询本人策略和今日摘要；完整财务月报仅 PARENT。Android 只上传本 App 的活跃分钟，不采集其他 App、通信或系统行为。
- Android 内存队列在同一进程断网重试时保持事件 UUID；刷新失败不覆盖最后成功快照。进程被系统终止后的待上传分钟不承诺恢复，见已知限制。

## 验证方式

| ID | 环境 | 结果 | 证据 |
| --- | --- | --- | --- |
| V9-01 | H2 PostgreSQL mode | 领域 12 项、Boot/API 11 项通过；PostgreSQL 专属 2 项按环境跳过 | `evidence/stage-9/acceptance.json` |
| V9-02 | PostgreSQL 16.15 | V1–V7、31 张生产表、23 项全量测试、跨域报告与权限反向通过 | `evidence/stage-9/acceptance.json` |
| V9-03 | Android JVM/lint/build | 16 项 JVM、lintDebug、debug/release 构建通过 | `evidence/stage-9/acceptance.json` |

## 完成标准

- [x] AC9-01 `PASS`：同键同载荷返回首次事件，载荷变化返回 409；今日 App/学习分钟与两条事实一致。
- [x] AC9-02 `PASS`：测试链取得 120.00 Money 收入、30.00 支出、10.00 储蓄和 0.10 基金费用并通过流水对账。
- [x] AC9-03 `PASS`：CHILD 月报 403，其他家庭访问今日摘要 404。
- [x] AC9-04 `PASS`：任务→奖励→礼金→兑换→储蓄→模拟基金→月报在 PostgreSQL 16.15 通过。
- [x] AC9-05 `PASS`：Android JVM 证明重试复用调用方幂等键；代码门禁保证刷新失败保留 `Connected` 快照，使用事件不修改 Ledger。

## 安全检查、已知限制与交接

使用统计是儿童敏感资料，最小采集且不可用于操纵留存；当前只保留业务所需的分钟事件。导出/删除和正式保留期限由 Stage 10 部署/隐私总验收明确。
