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

批准审核会锁定 Completion 与 Wallet；Coin/Money 流水先追加，再在同一事务更新余额和 Completion。重复提交返回首次结果；重复/冲突审核返回 409，不能重复发奖。XP 的奖励事实保存在 Completion 快照并更新 `child_progress`。

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

字段、校验、Bearer scheme 和完整响应以 [OpenAPI 3.1](../openapi.yaml) 为机器契约。Stage 4–7 的调账、兑换、商店、储蓄、愿望与基金接口尚未实现，不提前声明。
