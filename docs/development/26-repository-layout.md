# 仓库目录结构说明

| 路径 | 用途 | 是否可删除/重建 |
| --- | --- | --- |
| `AGENTS.md` | 每次对话的强制启动协议 | 不可删除或移动 |
| `docs/` | 需求、设计、Stage、手册、证据和治理事实源 | 不可整体删除 |
| `family-growth-domain/` | 无框架领域模型和规则 | 源码不可删除 |
| `family-growth-application/` | 用例和端口 | 源码不可删除 |
| `family-growth-infrastructure/` | JPA/Flyway/外部适配 | 源码不可删除 |
| `family-growth-web/` | REST 协议、DTO 和异常映射 | 源码不可删除 |
| `family-growth-boot/` | Spring Boot 启动与集成测试 | 源码不可删除 |
| `family-growth-android/` | Kotlin/Compose Android App | 源码不可删除 |
| `.github/workflows/` | CI/Release 自动化 | 变更需发布 Spec |
| `dist/` | 当前人工交付产物及说明 | 可由构建重建；删除前确认用户是否仍需下载 |
| `target/`、`*/build/` | Maven/Gradle 生成目录 | 可由对应 `clean` 重建 |

`docs/` 根目录只保留固定治理入口和 `openapi.yaml`；其他文档进入对应分类。证据不可因为重新构建而无记录删除。
