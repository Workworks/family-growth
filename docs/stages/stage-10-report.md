# Stage 10：V1 发布、部署与总验收

状态：`IN_PROGRESS`

产品 Phase：9　需求：REQ-001–009、REQ-013、REQ-018、REQ-021、REQ-022

## 目标与非目标

用户可观察目标：家庭可按手册部署 Spring Boot/PostgreSQL，安装稳定签名 APK，完成家长建任务→孩子提交→审核奖励→兑换/储蓄/模拟基金→报告→GitHub Release 更新的 V1 闭环。

范围内：生产配置门禁、数据库备份恢复演练、全量 H2/PostgreSQL/Android 构建、稳定签名 Release、V1 验收矩阵、限制收口和真机回放。范围外：云 SaaS、iOS、真实支付/投资和无系统确认的静默更新。

## 边界与不变量

- 没有真实 Android 设备时，构建、签名或模拟结果不得替代安装/触控/更新验收。
- 生产启动不得使用空凭据、H2、自动建表或公开无 TLS 服务。
- 数据库恢复演练使用隔离临时实例，不删除用户现有卷。
- Release 保持 applicationId、稳定证书、递增 versionCode/SemVer 和精确 digest。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP10-1 | 已完成 | prod 强制 PostgreSQL/非空凭据/TLS；Compose、非 root 镜像、备份和只恢复到新隔离库 |
| WP10-2 | 已完成 | 全量 H2/PostgreSQL 16.15/Android 自动化、Docker 镜像和跨域 E2E |
| WP10-3 | 进行中 | 本地稳定签名 0.3.0 APK 已验证；等待 Stage commit/tag 后创建 GitHub Release 并下载复验 |
| WP10-4 | 阻塞 | 真机首装、覆盖升级、数据保留、横屏/无障碍缺设备和可访问家庭服务 |
| WP10-5 | 已完成 | AC-V1-01–19 总矩阵逐项回链；未达到生产闭环的条目明确保持 PARTIAL/BLOCKED |

## 部署、发布与文档变化

- 新增 `prod` profile 与启动 guard：只接受 PostgreSQL、非空数据库用户名/密码、启用 TLS 及 `file:/classpath:` keystore；测试证明空密码、H2 和禁用 TLS 均拒绝。
- 新增 Dockerfile/Compose：PostgreSQL 不映射宿主端口，应用以非 root、只读根文件系统和 `no-new-privileges` 运行；`.dockerignore` 排除 Git、构建、APK、env 和 keystore。
- 新增 PowerShell 备份/恢复工具。恢复目标只允许新建 `family_growth_restore_*`；已存在目标不覆盖。隔离演练恢复 Flyway 标记与探针记录后清理全部临时资源。
- Android 版本提升到 0.3.0/versionCode 6；本地 APK 的包名、v2 稳定签名证书、大小和 SHA-256 已验证。GitHub Release 远端结果在发布后回填。
- 数据最小化与保留基线已明确；自动清理和家长自助导出/删除尚未实现，不能将 AC-V1-13 标记为完整通过。

## 验证方式

| ID | 环境 | 结果 | 证据 |
| --- | --- | --- | --- |
| V10-01 | H2 / PostgreSQL 16.15 | H2 12 领域 + 13 Boot（2 个 PG 环境跳过）；PG 12 + 13 全通过 | `evidence/stage-10/acceptance.json` |
| V10-02 | Docker Compose / pg_dump | Compose config、非 root 镜像、2,245 字节备份、隔离恢复与清理通过 | `evidence/stage-10/acceptance.json` |
| V10-03 | Android release | debug 16 JVM/lint/build；release 16 JVM/lint/build；0.3.0 v2 稳定签名通过 | `evidence/stage-10/acceptance.json` |
| V10-04 | Android 设备 | 未运行：无可用平板/模拟器和可访问服务 | `BLOCKERS.md` |

## 完成标准

- [x] AC10-01 `PASS`：H2/PostgreSQL 目标库、文档/JSON/OpenAPI 门禁通过。
- [x] AC10-02 `PASS`：生产配置 fail-closed；备份/恢复在新隔离库真实回放并清理资源。
- [ ] AC10-03 `IN_PROGRESS`：debug/release、本地稳定签名、版本、包名和 SHA-256 通过；待 GitHub Release/digest 下载复验。
- [x] AC10-04 `PASS（已实现自动化范围）`：AC-V1 矩阵逐项回链；AC-V1-11/13/17/19 的生产深度缺口未伪装为通过，保留为 PARTIAL。
- [ ] AC10-05 `BLOCKED`：真机首装、横屏、TalkBack、重启持久化和同签名覆盖升级缺 Android 设备。

## 安全检查、已知限制与交接

Stage 10 可离线工程已收口到发布步骤；只有在 GitHub Release 复验、真实设备与目标部署环境完成最终回放后才能 `COMPLETED`。AC-V1-11/13/17/19 当前分别只有本机零钱回收、PIN/最小日志、App 内分钟限制和透明费用基础实现；服务端冻结式兑现、自助数据权利、禁用时段/临时放行审计属于明确未完成深度，不得对外宣称完整生产能力。
