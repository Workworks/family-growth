# 密码与密钥

| Secret | 保存位置 | 禁止位置 |
| --- | --- | --- |
| 家长 PIN | 服务端强哈希，仅保存 hash | 明文数据库、日志、APK、聊天 |
| 数据库密码 | 环境变量或部署 secret | Git、文档示例、证据 |
| Android release keystore | 离线安全存储/CI secret | Git、Release 附件、聊天 |
| Keystore 密码/alias | CI secrets | Gradle 文件、日志 |
| GitHub Token | V1 公共仓库不需要 | APK BuildConfig、Git、日志 |

GitHub Actions 使用 `ANDROID_KEYSTORE_BASE64`、`ANDROID_STORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`；这些值只在仓库 secrets 中配置，不在本项目文件记录真实内容。
