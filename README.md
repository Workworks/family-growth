# Family Growth

儿童成长培养与家庭财商教育平台。Stage 3–7/9 已交付 PostgreSQL 生产闭环，Stage 10 已完成 TLS/凭据 fail-closed、备份恢复和本地稳定签名 0.3.0；Android 真机覆盖升级验收仍待完成。

快速入口：

- [GitHub 公共仓库](https://github.com/Workworks/family-growth)
- [文档索引](docs/README.md)
- [当前阶段](docs/stage-current.md)
- [待办账本](docs/TODO.md)
- [Stage 10 发布与总验收](docs/stages/stage-10-report.md)
- [产品设计](docs/design/01-product-design.md)
- [实施计划](docs/design/07-implementation-plan.md)

当前工程形态：五模块 Java 17 / Spring Boot 3 后端 + `family-growth-android` Kotlin/Compose 平板 App。

```powershell
.\mvnw.cmd test
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

当前本地稳定包见 `dist/family-growth-0.3.0.apk`；v0.3.0 GitHub Release 会在 Stage 10 commit/tag 后创建。0.2.0–0.3.0 使用同一专用 release 证书；没有可用 Android 设备时，仍不得描述为已完成真机覆盖升级或 V1 总验收。
