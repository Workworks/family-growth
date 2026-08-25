# Stage 10：V1 发布、部署与总验收

状态：`NOT_STARTED`

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
| WP10-1 | 待开始 | 部署配置、备份恢复和运维门禁 |
| WP10-2 | 待开始 | 全量后端/数据库/Android 自动化与 E2E |
| WP10-3 | 待开始 | 稳定签名 APK、GitHub Release 与下载复验 |
| WP10-4 | 待开始 | 真机首装、覆盖升级、数据保留、横屏/无障碍 |
| WP10-5 | 待开始 | AC-V1-01–19 总矩阵和已知限制收口 |

## 完成标准

- [ ] AC10-01 H2 隔离、PostgreSQL 目标库、文档/JSON/OpenAPI 门禁通过。
- [ ] AC10-02 生产配置 fail-closed，备份/恢复在隔离库可回放。
- [ ] AC10-03 debug/release、稳定签名、版本、包名、digest 和 GitHub Release 通过。
- [ ] AC10-04 AC-V1-02–19 自动化可覆盖部分全部通过且证据逐项可追。
- [ ] AC10-05 真机首装、横屏、TalkBack、重启持久化和同签名覆盖升级通过；缺设备时 Stage 必须 BLOCKED。

## 安全检查、已知限制与交接

Stage 10 只有在真实设备与目标部署环境完成最终回放后才能 `COMPLETED`；Agent 可先完成全部离线工程和 Release，外部阻塞单独列明。
