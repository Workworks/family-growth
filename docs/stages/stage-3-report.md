# Stage 3：生产认证、TaskCompletion、审核与奖励闭环

状态：`IN_PROGRESS`

产品 Phase：2　需求：REQ-001、REQ-002、REQ-005、REQ-020

## 目标与非目标

用户可观察目标：家庭可安全初始化家长 PIN；家长和孩子取得有时效的服务端会话；孩子提交任务完成记录，家长审核后 XP/Coin/Money 只发放一次，并能查询完成记录与奖励流水。

范围内：BCrypt PIN、失败锁定、Bearer 会话哈希、PARENT/CHILD RBAC、家庭/孩子对象权限、TaskCompletion 状态机、审核快照、三类奖励、奖励所需最小 Wallet/Ledger 原子写入、幂等键和审计字段。

范围外：复杂多家长邀请、密码找回、互联网身份提供商、图片对象存储、Android 联调、Stage 4 的通用调账/对账接口。

## 边界与不变量

- 除 bootstrap/login 外，`/api/v1` 业务接口默认无有效 Bearer 会话即 401；角色禁止为 403；跨家庭和无权对象统一 404。
- PIN 只保存 BCrypt 哈希；会话只保存 SHA-256 哈希，原 token 仅在创建响应返回一次；日志、证据和 Git 不保存 secret。
- Completion 仅允许 `SUBMITTED → APPROVED | REJECTED`；重复审核不得重复发奖。
- Coin/Money 余额变化必须与不可变 LedgerEntry 同事务提交；余额不得为负。
- `Idempotency-Key` 对关键写入必填并以家庭、操作类型和 key 唯一。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP3-1 | 进行中 | 生产认证、PIN 锁定、会话与服务端角色上下文 |
| WP3-2 | 待开始 | TaskCompletion 领域状态机、迁移和持久化 |
| WP3-3 | 待开始 | 审核、XP/Coin/Money 奖励与最小账本原子事务 |
| WP3-4 | 待开始 | REST/OpenAPI、401/403/404/409 和幂等集成测试 |
| WP3-5 | 待开始 | PostgreSQL 目标库验证、证据和 Stage commit |

## 数据与 API 变化

新增迁移只追加，不修改 V1。计划新增 `parent_pin_credential`、`auth_session`、`task_completion`、`child_progress`、`wallet`、`ledger_entry`、`idempotency_operation`。新增 bootstrap/login/child-session、completion submit/review/query 与 wallet/ledger read API。

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V3-01 | JVM | PIN、状态机、金额精度和奖励测试 | 哈希不泄露，非法转换拒绝，精度稳定 | Surefire |
| V3-02 | Spring + H2 PostgreSQL mode | 完整 bootstrap→child→task→submit→approve | 三奖励与流水只出现一次 | API integration |
| V3-03 | Spring | 缺 token、CHILD 审核、跨 family、重复 key | 401/403/404/409 且无副作用 | negative integration |
| V3-04 | PostgreSQL 16 | Flyway、Hibernate validate、并发/唯一约束 | 目标库通过 | `evidence/stage-3/acceptance.json` |

## 完成标准

- [ ] AC3-01 bootstrap/login 使用 BCrypt PIN，连续失败触发限时锁定，会话持久化为哈希。
- [ ] AC3-02 孩子只能为本人和本家庭任务提交 Completion，非法对象返回 404。
- [ ] AC3-03 家长批准一次后 XP/Coin/Money 与 Ledger 原子一致，重复审核或重放不重复发放。
- [ ] AC3-04 401/403/404/409、Bean Validation 与统一错误结构自动化通过。
- [ ] AC3-05 PostgreSQL 16 迁移、约束和集成门禁通过并形成可回放证据。

## 安全检查、已知限制与交接

生产认证是本 Stage 的组成部分，不允许用请求头自报角色代替。bootstrap 只创建新家庭首位家长；后续家长邀请在 Stage 8 后深化。Android 仍使用 Stage 13 本地引擎，服务端联调属于 Stage 8–9。
