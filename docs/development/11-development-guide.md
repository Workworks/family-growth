# 开发指南

## 技术基线

- 后端：Java 17、Spring Boot 3.4、Maven 多模块、Flyway、JPA、PostgreSQL。
- Android：Kotlin、Compose、Material 3、MVVM，minSdk 26、targetSdk 34。
- 依赖方向：`domain ← application ← infrastructure/web ← boot`。

## 开发流程

1. 按根 [AGENTS](../../AGENTS.md)加载事实源并检查工作树。
2. 新行为先登记需求并补目标 Stage Spec。
3. 在工作包边界内实现；数据库只新增 Flyway migration。
4. 按 [测试计划](12-test-plan.md)运行定向和集成门禁。
5. 同步 API、OpenAPI、Android 手册、Stage 和证据。
6. 执行 diff、空白、链接、JSON/YAML 解析并按固定格式汇报。

## 编码约束

- 金额使用 `BigDecimal`，禁止 `double/float`。
- Controller 不放领域规则；DTO、实体和领域模型分离。
- 构造器注入，业务时间 UTC，API 使用稳定错误码。
- Money/Coin 变化只能经 LedgerEntry；费用预览与确认分离。
- Android 页面必须显示 loading/error/empty/权限状态，不使用固定成功。
