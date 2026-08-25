# Android 开发指南

## 基线与布局

- Kotlin + Jetpack Compose + Material 3；一个 APK 支持 PARENT/CHILD。
- 平板横屏为第一验收基线，手机为兼容布局。
- 业务状态使用 MVVM/Repository；服务端是认证、权限和账本最终事实源。

## 构建

```powershell
cd family-growth-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## 运行态验收

1. `adb devices` 必须为 `device`，不能是 offline。
2. 安装 APK，启动主 Activity，回放家长/孩子展示。
3. 平板横竖屏切换无崩溃，保留截图与 logcat 摘要。
4. 涉及更新时使用同一 release 签名的低/高 versionCode 回放覆盖升级。

## 更新安全

GitHub Release 更新规则、资产命名和签名 secrets 见 [发布手册](../operations/01-github-release-update.md)。不得关闭 SHA-256、包名、版本、签名或系统安装确认门禁。
