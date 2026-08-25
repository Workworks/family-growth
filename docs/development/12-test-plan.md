# 测试计划

## 分层门禁

| 层级 | 最低要求 |
| --- | --- |
| Domain | 规则、不变量、金额精度、非法状态 |
| Application | 用例编排、家庭/孩子对象权限、幂等 |
| Spring/API | 正向、校验、401/403/404、冲突、并发 |
| Database | H2 隔离 + PostgreSQL Flyway/Hibernate validate |
| Android | JVM、Compose UI、lint、构建、签名/版本 |
| 安装/更新 | 真机首装、旋转、同签名覆盖升级、数据保留 |
| E2E | [V1 端到端场景](../manuals/scenarios/family-growth-v1-e2e.md) |

## 通用命令

```powershell
.\mvnw.cmd test
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

门禁选择必须与变更风险匹配。未执行、目标环境缺失或只用 Mock 时不得写 PASS。
