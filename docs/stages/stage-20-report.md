# Stage 20：学段底座、家长配置与分层体验路由

状态：`BLOCKED`

产品 Phase：全面教学基座

需求：REQ-021、REQ-028、REQ-030、REQ-031、REQ-032

## 目标与非目标

家长可以为孩子配置出生日期、查看系统推荐学段，并在 PIN/RBAC 保护下覆盖为幼儿园、小学、初中或高中；孩子不能修改。Android 根据同一份有效学段显示不同的信息结构和交互反馈，离线时保留明确标识的本机安全配置。家长还可以登记经过权利审查的纪录片来源，未批准、已撤回或仅允许家长打开的来源不会变成儿童开放网页入口。

本 Stage 不建设完整 Course/Unit/Lesson/Activity、题库、掌握度和内容包下载，这些属于 Stage 21–25；不下载、转码或再分发任何第三方纪录片；不把四个学段骨架页面冒充完整教学内容。

## 边界与不变量

- 服务端是连接状态下年龄、推荐学段、覆盖学段和权利目录的事实源；本机配置只供未连接时使用并明确标识。
- 默认映射为 0–2 岁 `PARENT_ONLY`、3–5 岁 `KINDERGARTEN`、6–11 岁 `PRIMARY`、12–14 岁 `JUNIOR_MIDDLE`、15 岁及以上 `SENIOR_HIGH`；最终学段等于合法覆盖值或推荐值。
- 只有 PARENT 可修改出生日期、学段覆盖、触觉开关和纪录片目录；同家庭孩子只可读取自己的有效体验配置，跨家庭统一 404。
- 学段变更追加不可变审计，不删除/重算历史任务、奖励或学习记录。
- 幼儿园主动作反馈不超过 320ms、最多两次短触觉；普通儿童控件有明确按下态和单次确认。系统关闭动画/触觉时安全降级，不循环震动、闪烁或随机庆祝。
- 纪录片条目必须包含学段、访问模式、权利方/权利依据、审核状态和撤回能力；`OFFICIAL_LINK` 永远要求家长打开，儿童响应不返回可启动 URL。
- 不修改 Wallet/Ledger，不接真实金融，不记录 PIN/Token/儿童敏感信息到日志或证据。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP20-1 | 已完成 | Spec、REQ-032、Stage/路线图/当前状态和 V9 迁移 |
| WP20-2 | 已完成 | SchoolStage、体验配置、反馈档案、审计领域与应用服务 |
| WP20-3 | 已完成 | JDBC Store、REST API、OpenAPI、401/403/404/校验/并发测试 |
| WP20-4 | 已完成 | 纪录片权利目录、家长审批/撤回与儿童安全投影 |
| WP20-5 | 已完成 | Android 本机/远端配置、家长设置页、四学段路由和儿童反馈引擎 |
| WP20-6 | 外部阻塞 | H2/PostgreSQL、Android 自动化、文档和证据已完成；目标平板回放未执行 |

## 数据、API、Android 与文档变化

- Flyway V9 新增 `child_experience_profile`、`child_experience_audit`、`documentary_source`、`documentary_source_action` 及家庭/孩子/状态索引和约束；不修改旧迁移。
- 新增 `GET/PUT /api/v1/families/{familyId}/children/{childId}/experience-profile`、家长审计查询和纪录片来源创建/查询/撤回 API。
- Android 新增 `SchoolStage`、`ChildExperienceSettings`、`FeedbackProfile`，家长可在本机或已连接服务端配置；孩子首页按四学段使用不同结构。
- 同步更新 `docs/openapi.yaml`、API 参考、用户手册、Stage/TODO/current/BLOCKERS/roadmap 和需求账本。

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V20-01 | Java 领域 | 年龄边界、覆盖、反馈档案、URL/权利状态向量 | 推荐/有效学段和安全投影精确，非法组合拒绝 | Stage 20 evidence |
| V20-02 | H2 MockMvc | 家长读取/修改/审计，孩子读取/修改，跨家庭、校验、纪录片批准/撤回 | 200/201、401/403/404/409/400 与审计正确 | Stage 20 evidence |
| V20-03 | PostgreSQL 16 | V1–V9、Hibernate validate、Stage 20 与全量后端测试 | 迁移、约束、索引、事务和 API 通过 | Stage 20 evidence |
| V20-04 | Android JVM/Compose/lint/build | 四学段策略、旧 JSON 兼容、远端解析、家长配置、反馈降级与 debug/release | 自动化、lint 和两变体构建通过 | Stage 20 evidence |
| V20-05 | Android 平板 | 切换四学段、横竖屏、触觉开关、Reduced Motion、TalkBack、重启和服务端同步 | 真实交互与持久化通过 | Stage 20 evidence/设备阻塞 |
| V20-06 | 通用 | diff、secret、Markdown 本地链接、证据 JSON、OpenAPI | 治理、文档与机器契约一致 | Stage 20 evidence |

## 完成标准

- [x] AC20-01：`PASS` — 服务端根据出生日期计算推荐学段，家长可覆盖/清除覆盖和配置触觉，孩子只读自己的配置，变更保留不可变审计。
- [x] AC20-02：`PASS` — 四学段返回不同能力与反馈档案；Android 四套首页具有不同信息结构，不是只换标题或颜色。
- [x] AC20-03：`PASS_OFFLINE` — 幼儿园主要动作有短促明显动效，儿童控件有可见按下态/适龄触觉，并在系统关闭时降级；真实触觉质量归 AC20-06。
- [x] AC20-04：`PASS` — 纪录片目录强制权利模式、学段、批准/撤回；儿童响应不包含可启动 URL 或权利元数据。
- [x] AC20-05：`PASS` — H2、PostgreSQL 16.15、Android JVM/lint/debug/release、OpenAPI/文档门禁通过且证据可解析。
- [ ] AC20-06：`BLOCKED` — 目标 Android 平板的触觉、Reduced Motion、TalkBack、布局、重启和服务端同步需要真实设备回放。

## 安全检查、已知限制与交接

- 出生日期属于儿童资料，API/日志/证据最小展示；审计只保存必要变更值和操作者 ID。
- `OFFICIAL_LINK` 仅允许家长操作；Stage 20 不证明第三方视频已获离线分发、翻译或剪辑授权。
- 本机无法建立可用 Android Emulator ADB，V20-05 在设备可用前保持 `BLOCKED`；不得以 Compose/JVM 或 APK 构建替代。
- 可离线工程已收口，命令、结果和未执行项见 [Stage 20 证据](../evidence/stage-20/acceptance.json)。Stage 仍因 AC20-06 保持 `BLOCKED`。
- Stage 21 可在稳定学段与权利模型上建设课程/活动/题库/证据引擎；其立项不得把 Stage 20 真机缺口静默视为已通过。
