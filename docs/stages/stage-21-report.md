# Stage 21：共用课程、活动与学习证据引擎

状态：`IN_PROGRESS`

产品 Phase：全面教学核心

需求：REQ-021、REQ-028、REQ-030、REQ-031、REQ-033

## 目标与非目标

建立幼儿园、小学、初中、高中共用的生产教学事实链：家长可创建并发布带版本和权利依据的 `Subject → Course → Unit → Lesson → Activity`，把课节分配给孩子；孩子按活动规则提交真实尝试；系统分别记录 `VIEWED / ATTEMPTED / CHECKED / PARENT_CONFIRMED / MASTERED`，家长可确认完成或要求复做。Android 连接家庭服务后读取真实课程、恢复进度并提交活动，而不是继续依赖硬编码视频目录。

本 Stage 不制作足量学科内容，不实现四学段深度模板（Stage 22–25），不实现内容包下载/签名/断点续传，不允许开放儿童网页，不以视频播放时间代表掌握，不接真实金融。Stage 20 的纪录片来源目录继续作为来源审批事实，不在本 Stage 复制第二套来源表。

## 边界与不变量

- 服务端是课程版本、发布状态、分配、尝试、完成和证据的事实源；Android 本机只缓存安全投影和待同步操作，不生成服务端掌握结论。
- 已发布内容不可原地改写；修改需创建新版本。历史 Assignment/Attempt/Evidence 永远引用当时版本，不被新版本重算。
- 活动类型至少覆盖 `SHORT_VIDEO`、`PARENT_CHILD_READING`、`LISTEN_CHOOSE`、`SINGLE_CHOICE`、`MATCHING`、`SORTING`、`ORAL_RESPONSE`、`OFFLINE_PRACTICE`、`PARENT_CONFIRMATION`。
- `SHORT_VIDEO` 达到内容规则只产生 `VIEWED`；选择/配对/排序可产生 `CHECKED`；口头、阅读和现实活动默认需要家长确认。只有满足课节明确的 required evidence 后才可 `MASTERED`。
- 状态机为 `ASSIGNED → IN_PROGRESS → SUBMITTED → COMPLETED`，家长可将 `SUBMITTED → REWORK_REQUIRED`，孩子可将 `REWORK_REQUIRED → IN_PROGRESS/SUBMITTED`。完成和复做动作幂等，禁止跳过 required evidence。
- CHILD 只能读取自己的已发布、已分配且学段匹配内容并提交自己的尝试；PARENT 管理内容、分配、审核和报告；跨家庭统一 404。
- 不采集儿童人脸、声音或精确位置。口头/线下活动 V1 只保存最小化文本说明和家长确认，不上传录音录像。
- 本 Stage 不直接改变 Money/Coin。后续奖励必须复用现有同事务 Ledger 不变量，不能从学习证据旁路加钱。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP21-1 | 进行中 | Spec、REQ-033、路线图、当前状态、TODO/BLOCKERS 和 V10 数据模型 |
| WP21-2 | 未开始 | Course/Unit/Lesson/Activity、内容版本、发布与权利投影领域模型 |
| WP21-3 | 未开始 | Assignment/Attempt/Completion/MasteryEvidence 状态机、幂等和权限服务 |
| WP21-4 | 未开始 | JDBC Store、REST API、OpenAPI 与 H2/PostgreSQL 权限/并发测试 |
| WP21-5 | 未开始 | Android 真实课程读取、活动提交、进度恢复、401/离线/冲突处理 |
| WP21-6 | 未开始 | 全量门禁、文档、证据、平板/E2E 限制与 Stage 提交 |

## 数据、API、Android 与文档变化

- Flyway V10 只新增教学表和索引，不修改 V1–V9。预期实体包括 subject/course/course_version/course_unit/lesson/learning_activity/question/question_option/lesson_assignment/activity_attempt/learning_completion/mastery_evidence/content_action。
- API 采用 `/api/v1/families/{familyId}/teaching/...` 家长管理面和 `/children/{childId}/learning/...` 儿童执行面；所有写入需要幂等键，内容修改/完成审核需要版本检查。
- Android 在 Stage 20 的三入口和学段路由内渲染服务端活动；不增加儿童一级导航。离线队列只保存最小提交载荷并在重启后恢复，冲突时不静默覆盖服务端结果。
- 同步更新 OpenAPI、API 参考、数据库/架构设计、用户手册、Stage 账本和 `docs/evidence/stage-21/acceptance.json`。

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V21-01 | Java 领域 | 内容版本、活动校验、证据要求、完成/复做状态向量 | 非法类型/跳转/缺证据拒绝；已发布版本不被改写 | Stage 21 evidence |
| V21-02 | H2 MockMvc | 家长建课/发布/分配，孩子目录/尝试/提交，家长确认/复做，RBAC/跨家庭/幂等/版本冲突 | 200/201、400/401/403/404/409 和证据链准确 | Stage 21 evidence |
| V21-03 | PostgreSQL 16 | V1–V10、Hibernate validate、并发尝试/审核和全量后端测试 | 迁移、约束、唯一键、锁和状态机通过 | Stage 21 evidence |
| V21-04 | Android JVM/lint/build | 远端课程解析、学段过滤、活动提交、重启 outbox、401/409、debug/release | 自动化、lint 和两变体构建通过 | Stage 21 evidence |
| V21-05 | Android 平板 + 测试服务 | 四学段读取同一引擎不同投影，完成选择/视频/亲子/线下活动，断网重启恢复，家长复做/确认 | 真实 UI、网络、持久化、无障碍和 E2E 通过 | Stage 21 evidence/设备阻塞 |
| V21-06 | 通用 | diff、secret、Markdown 链接、证据 JSON、OpenAPI | 治理与机器契约一致 | Stage 21 evidence |

## 完成标准

- [ ] AC21-01：`NOT_RUN` — 已发布课程版本不可变；孩子只获得有效学段、已发布、已分配内容，跨家庭和未发布内容不可见。
- [ ] AC21-02：`NOT_RUN` — 九类活动具有明确输入/完成规则；视频只产生 VIEWED，客观题保存题目版本/答案/提示，亲子与现实活动要求家长确认。
- [ ] AC21-03：`NOT_RUN` — Assignment/Attempt/Completion/Evidence 构成可回放事实链，支持 REWORK_REQUIRED、再次提交、幂等和并发冲突保护。
- [ ] AC21-04：`NOT_RUN` — Android 不新增儿童一级导航，能从服务端读取/提交/恢复并在离线、401、409 和进程重启下给出可恢复状态。
- [ ] AC21-05：`NOT_RUN` — H2、PostgreSQL 16、Android JVM/lint/debug/release、OpenAPI/文档门禁通过且证据可解析。
- [ ] AC21-06：`BLOCKED` — 目标平板上的多活动交互、TalkBack、横竖屏、断网重启与家长/孩子端到端需要真实设备和测试服务。

## 安全检查、已知限制与交接

- 内容权利依据、发布人、版本和下架动作必须审计；儿童响应不包含权利文档、任意 URL、答案键或其他孩子数据。
- 题目答案只在服务端判定，Android 不下载可直接还原的答案键；自由文本做长度/日志脱敏，不做自动人格或心理标签推断。
- Stage 20 因真机回放保持 `BLOCKED`，不阻止 Stage 21 的独立后端/自动化工作；两者可在同一设备窗口联合验收，但状态分别回填。
- Stage 22–25 只有在本引擎事实链稳定后才制作各学段深度页面和内容；Stage 26 负责连续性、隐私、平板和 Release 总验收。
