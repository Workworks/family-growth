# 本地部署（开发态）

## 后端

```powershell
.\mvnw.cmd test
.\mvnw.cmd -pl family-growth-boot -am spring-boot:run
```

目标数据库部署应使用 PostgreSQL；连接配置通过环境变量提供，不提交密码。当前 API 无生产认证，只用于受控开发环境。

## Android

```powershell
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

将 APK 安装到与后端网络可达的 Android 平板。实际后端地址配置、TLS 和家庭首次初始化仍属于后续部署 Stage。
