# 运维指南

## 当前可运维范围

- Maven/Gradle 全量构建、测试、lint 和签名检查；
- PostgreSQL 16 Flyway V1–V7 迁移与 Hibernate validate；
- Docker Compose 家庭局域网部署、TLS fail-closed 和健康检查；
- PostgreSQL custom-format 备份及只允许新建 `family_growth_restore_*` 隔离库的恢复；
- 稳定签名 APK、SHA-256、GitHub Release 和下载复验。

## 运行原则

- 数据库迁移只前进，不修改已执行脚本。
- 日志不得记录 PIN、儿童敏感资料、Token、数据库密码或签名 secret。
- 升级前确认数据库备份和 Android 同签名/versionCode；失败不得伪造成功状态。
- 健康端点只暴露 `health/info` 且不显示详情；日志和指标不得以儿童活跃、留存或消费作为优化目标。

## 备份与恢复

```powershell
& .\deploy\Backup-FamilyGrowth.ps1 -DestinationDirectory E:\FamilyGrowthBackups -EnvironmentFile .env
& .\deploy\Restore-FamilyGrowth.ps1 -BackupFile E:\FamilyGrowthBackups\family-growth-YYYYMMDD-HHMMSS.dump -TargetDatabase family_growth_restore_drill -EnvironmentFile .env
```

备份脚本返回路径、字节数和 SHA-256。恢复脚本拒绝生产库名和任意名称，只能新建匹配 `family_growth_restore_*` 的数据库；若目标已存在立即失败，不覆盖。恢复验证后由管理员按明确维护窗口决定是否切换连接，本脚本不自动替换生产库。

建议每天备份并至少保留 7 个每日、4 个每周、12 个月度副本；备份应加密并与数据库主机分离。每季度在隔离库恢复一次并记录 Flyway 版本、业务行数和耗时。
