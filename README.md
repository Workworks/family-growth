# Family Growth

儿童成长培养与家庭财商教育平台。Stage 14 已交付公共仓库、稳定 release 签名及 v0.2.0→v0.2.1 GitHub Release 更新链；生产服务端深度闭环和 Android 真机覆盖升级验收仍待完成。

快速入口：

- [GitHub 公共仓库](https://github.com/Workworks/family-growth)
- [文档索引](docs/README.md)
- [当前阶段](docs/stage-current.md)
- [待办账本](docs/TODO.md)
- [Stage 13 基础体验报告](docs/stages/stage-13-report.md)
- [产品设计](docs/design/01-product-design.md)
- [实施计划](docs/design/07-implementation-plan.md)

当前工程形态：五模块 Java 17 / Spring Boot 3 后端 + `family-growth-android` Kotlin/Compose 平板 App。

```powershell
.\mvnw.cmd test
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

当前稳定包见 `dist/family-growth-0.2.1.apk`，公开发布于 [GitHub Release v0.2.1](https://github.com/Workworks/family-growth/releases/tag/v0.2.1)。0.2.0/0.2.1 使用同一专用 release 证书并通过公开 digest、包信息与签名门禁；没有可用 Android 设备时，仍不得描述为已完成真机覆盖升级或 V1 总验收。
