# 本地开发

## 后端

前置：Java 17。构建只使用仓库 Wrapper。

```powershell
.\mvnw.cmd test
.\mvnw.cmd -pl family-growth-boot -am spring-boot:run
```

默认隔离测试可使用 H2；正式验收使用 PostgreSQL，环境变量和凭据不得提交。当前 API 尚无生产认证，不应绑定到不受信网络。

## Android

前置：Android SDK、JDK 17。

```powershell
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。构建成功不代表安装启动通过，目标设备验收见 [Android 开发](34-android-development.md)。
