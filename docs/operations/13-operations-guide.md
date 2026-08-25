# 运维指南

## 当前可运维范围

- Maven/Gradle 构建和测试；
- PostgreSQL Flyway V1 迁移验证；
- debug APK 产物与哈希检查；
- GitHub Release workflow 配置检查。

## 运行原则

- 数据库迁移只前进，不修改已执行脚本。
- 日志不得记录 PIN、儿童敏感资料、Token、数据库密码或签名 secret。
- 升级前确认数据库备份和 Android 同签名/versionCode；失败不得伪造成功状态。
- 当前无完整备份恢复与生产监控，正式运维能力属于后续 Stage。
