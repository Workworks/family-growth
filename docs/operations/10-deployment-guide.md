# 部署指南

状态：Stage 10 可离线部署包已完成；目标家庭网络和 Android 平板运行态仍需现场验收。

目标形态是家庭局域网中的 Spring Boot + PostgreSQL 16 后端，以及同一家庭使用的 Android APK。生产 profile 强制 PostgreSQL、非空数据库凭据和 TLS keystore，任一缺失即拒绝启动；服务端已使用 Bearer 会话、家长 PIN、RBAC 和家庭/孩子对象权限。

## Docker Compose 部署

1. 安装 Docker Desktop/Engine 与 Compose v2，把 [环境模板](../../deploy/.env.example)复制为 `deploy/.env`。
2. 为数据库和 PKCS12 keystore 生成不同的长随机密码；`FAMILY_GROWTH_TLS_KEYSTORE_HOST_PATH` 必须是宿主绝对路径。不要把 `.env`、keystore 或密码提交到 Git。
3. TLS 证书的主机名/IP 必须与平板访问地址匹配，并由平板信任；V1 不提供跳过证书校验。
4. 在仓库根执行：

```powershell
docker compose --project-directory deploy --env-file deploy/.env config
docker compose --project-directory deploy --env-file deploy/.env up -d --build
docker compose --project-directory deploy --env-file deploy/.env ps
```

5. 从受信终端检查 `https://<家庭服务地址>:8443/actuator/health` 返回 `UP`，再由家长在 App 的“家庭服务连接”输入 HTTPS 根地址和服务端 UUID。

Compose 不向宿主暴露 PostgreSQL 端口，应用容器为非 root、只读根文件系统，并启用 `no-new-privileges`。这降低误暴露风险，但不代替路由器防火墙、系统更新和家庭网络隔离。

## 升级与回滚

- 升级前先按 [运维指南](13-operations-guide.md)生成并校验备份，再拉取已审核的 commit/tag 并重建 app；Flyway 只前进。
- 后端失败时保留数据库卷与备份，回到兼容的应用镜像；禁止回改已执行迁移。
- Android 只使用同一稳定证书、递增 versionCode 的 GitHub Release，由系统界面让用户确认安装。

开发态步骤见 [本地部署](22-local-deployment.md)，数据保留边界见 [数据保留与恢复](23-data-retention-and-recovery.md)。
