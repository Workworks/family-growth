# Stage 32：Android 工具链与最终全量治理验收

状态：`IN_PROGRESS`

产品 Phase：工程治理 / 最终稳定发布

需求：REQ-051、REQ-052；Backlog P-21

## 用户可观察目标

消除 compileSdk 36 与旧 AGP 的兼容告警，在不改变业务、安全边界和稳定签名身份的前提下完成跨 Stage 总审计，发布包含 Stage 27–32 工程能力的新稳定 APK。

## 技术决策与边界

- 采用 Android 官方明确支持 API 36.1 的 AGP 8.13.2、Gradle 8.13、JDK 17；版本固定，不使用动态依赖。官方依据：[AGP 8.13 release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes)、[AGP/Gradle compatibility](https://developer.android.com/build/releases/about-agp)。
- 保持 Kotlin 2.0.21、KSP、Room 2.8.4、compileSdk 36、targetSdk 34 与 minSdk 26；本 Stage 不借工具链升级改变 Android 平台行为或扩大权限。
- Wrapper 固定 Gradle 8.13，并登记官方发行包 SHA-256；构建不得忽略校验。
- 版本计划为 `0.3.9` / `versionCode 15`，复用现有 GitHub Actions 稳定签名与公开 Release 更新契约。不得把本地 unsigned release、构建成功或远端 Release 冒充目标平板覆盖升级。
- 最终审计覆盖 H2、隔离 PostgreSQL 16.15、Android 双变体、lint、APK 元数据/签名、OpenAPI、文档链接、证据 JSON、secret 扫描、Git 状态和公开 Release digest。

## 工作包

| WP | 内容 | 完成定义 |
| --- | --- | --- |
| WP32-1 | 工具链升级 | AGP 8.13.2 / Gradle 8.13 / JDK 17 生效，compileSdk 36 兼容告警消失 |
| WP32-2 | 跨 Stage 全量门禁 | 双数据库、Android 测试/lint/双 APK、契约/文档全部真实通过 |
| WP32-3 | 版本与发布 | 0.3.9/15 使用稳定证书由 tag workflow 发布，公开 digest/大小/包名/版本/证书一致 |
| WP32-4 | 最终治理审计 | TODO、BLOCKERS、Stage、需求账本、手册与证据只保留真实外部阻塞 |

## 完成标准

- [x] AC32-01：目标、技术版本、兼容依据、发布边界和验证矩阵已写清。
- [x] AC32-02：AGP 8.13.2 / Gradle 8.13 / JDK 17 同步，112 项 JVM、lint、debug/release 构建通过，旧 compileSdk 36 兼容告警消失。
- [x] AC32-03：H2/PostgreSQL 16.15 全量均通过；OpenAPI、链接、JSON、secret 与 diff 门禁通过。
- [ ] AC32-04：v0.3.9/15 稳定签名 Release 的 workflow 与公开资产身份复验通过。
- [ ] AC32-05：最终账本审计完成；目标平板/可信服务等外部条件继续明确 `BLOCKED`。

结构化证据见 [Stage 32 acceptance](../evidence/stage-32/acceptance.json)。

## 本地与数据库验证事实

| 门禁 | 结果 |
| --- | --- |
| 工具链 | AGP 8.13.2 / Gradle 8.13 / JDK 17；Wrapper 官方 SHA-256 校验通过；API 36 兼容告警消失 |
| H2 Maven | 73 tests，0 failure/error；6 个 PostgreSQL 条件测试按预期跳过 |
| PostgreSQL 16.15 | 28 migrations / 106 production tables / 73 tests，0 failure/error/skip；临时容器已删除 |
| Android | 112 JVM tests，0 failure/error/skip；lint 0 error / 6 warning；debug/release APK 通过 |
| APK 元数据 | unsigned release 为 `com.familygrowth.android`、0.3.9/15、minSdk 26、compileSdk 36；正式签名仅由 Release workflow 生成 |
| 文档与契约 | OpenAPI 3.1 / 127 paths、101 个 Markdown 本地链接、32 个 Stage 证据 JSON、diff/secret 扫描均通过 |

保留的 6 个 lint warning 是显式治理项：targetSdk 34 暂不随工具链改变平台行为；Gradle 8.13 是 AGP 8.13 官方默认版本；两项 Lifecycle 维持已验证兼容基线；两项启动图标 XML/PNG 同名用于自适应与 OEM 回退。它们不是构建错误，也不包含被忽略的 API 36 兼容警告。
