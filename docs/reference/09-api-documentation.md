# API 文档

基础路径：`/api/v1`。当前只实现 Stage 2 的六个创建接口，尚未实现生产认证；不得视为可部署的完整 API。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/families` | 创建家庭 |
| POST | `/families/{familyId}/parents` | 添加家长资料 |
| POST | `/families/{familyId}/children` | 添加孩子资料 |
| POST | `/families/{familyId}/children/{childId}/plans` | 创建成长计划 |
| POST | `/families/{familyId}/plans/{planId}/goals` | 创建成长目标 |
| POST | `/families/{familyId}/goals/{goalId}/tasks` | 创建成长任务 |

成功返回 HTTP 201 和 `{data,error,traceId}` 包装；校验失败返回 400/`VALIDATION_FAILED`，资源不存在或跨家庭对象返回 404/`RESOURCE_NOT_FOUND`。请求字段和 schema 以 [OpenAPI](../openapi.yaml) 为机器契约。

认证、查询、更新、任务审核、钱包和基金接口尚未实现，不在 OpenAPI 中提前声明。
