# 本地部署（开发态）

## 后端

```powershell
.\mvnw.cmd test
.\mvnw.cmd -pl family-growth-boot -am spring-boot:run
```

开发/测试仍使用 PostgreSQL；连接配置通过环境变量提供，不提交密码。生产必须激活 `prod` profile，并使用 [部署指南](10-deployment-guide.md)要求的 TLS、非空凭据和备份门禁。

## Android

```powershell
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

将 APK 安装到与后端网络可达且信任服务证书的 Android 平板。release 构建禁止明文 HTTP；debug 仅允许 loopback/私网字面地址用于开发。
