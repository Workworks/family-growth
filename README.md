# Family Growth

儿童成长培养与家庭财商教育平台。Stage 13 已交付 Stage 2–11 的 Android 本机基础体验宽度与 0.2.0 debug APK；生产服务端深度闭环和 Android 真机验收仍待完成。

快速入口：

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

当前交付包见 `dist/family-growth-0.2.0-debug.apk`。它已通过单测、lint、构建、包信息、v2 签名和 SHA-256 门禁，但仍是内部 debug 包；没有可用 Android 设备时，不得把它描述为已安装或已完成 V1 真机验收。
