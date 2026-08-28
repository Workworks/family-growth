# Stage 21：共用课程、活动与学习证据引擎

状态：`IN_PROGRESS`

产品 Phase：全面教学核心

需求：REQ-021、REQ-028、REQ-030、REQ-031、REQ-033、REQ-034、REQ-035、REQ-036、REQ-037、REQ-038

## 目标与非目标

建立幼儿园、小学、初中、高中共用的生产教学事实链：家长可创建并发布带版本和权利依据的 `Subject → Course → Unit → Lesson → Activity`，把课节分配给孩子；孩子既可完成家庭任务，也可进入自主学习；系统分别记录 `VIEWED / ATTEMPTED / CHECKED / PARENT_CONFIRMED / MASTERED`，家长可确认完成或要求复做。Android 连接家庭服务后读取真实课程、恢复进度并提交活动，而不是继续依赖硬编码视频目录。

本 Stage 不制作足量学科内容，不实现四学段深度模板（Stage 22–25），不实现内容包下载/签名/断点续传，不允许开放儿童网页，不以视频播放时间代表掌握，不接真实金融。Stage 20 的纪录片来源目录继续作为来源审批事实，不在本 Stage 复制第二套来源表。

## 边界与不变量

- 服务端是课程版本、发布状态、分配、尝试、完成和证据的事实源；Android 本机只缓存安全投影和待同步操作，不生成服务端掌握结论。
- 已发布内容不可原地改写；修改需创建新版本。历史 Assignment/Attempt/Evidence 永远引用当时版本，不被新版本重算。
- 活动类型至少覆盖 `SHORT_VIDEO`、`PARENT_CHILD_READING`、`LISTEN_CHOOSE`、`SINGLE_CHOICE`、`MATCHING`、`SORTING`、`ORAL_RESPONSE`、`OFFLINE_PRACTICE`、`PARENT_CONFIRMATION`。
- `SHORT_VIDEO` 达到内容规则只产生 `VIEWED`；选择/配对/排序可产生 `CHECKED`；口头、阅读和现实活动默认需要家长确认。只有满足课节明确的 required evidence 后才可 `MASTERED`。
- 状态机为 `ASSIGNED → IN_PROGRESS → SUBMITTED → COMPLETED`，家长可将 `SUBMITTED → REWORK_REQUIRED`；孩子必须产生至少一次晚于返工要求的新尝试，才可再次提交。完成和复做动作幂等，禁止跳过 required evidence 或原样立即重交。
- CHILD 只能读取自己的已发布、已分配且学段匹配内容并提交自己的尝试；PARENT 管理内容、分配、审核和报告；跨家庭统一 404。
- 不采集儿童人脸、声音或精确位置。口头/线下活动 V1 只保存最小化文本说明和家长确认，不上传录音录像。
- 本 Stage 不直接改变 Money/Coin。后续奖励必须复用现有同事务 Ledger 不变量，不能从学习证据旁路加钱。
- 国家中小学智慧教育平台入口固定为 HTTPS 官方域名；儿童先在 App 内按学段、年级、学科、版本、册次选择，再由受限 WebView 直接加载官方页面。WebView 禁止文件访问、第三方 Cookie、任意域名跳转和自动播放；不抓取 m3u8/MP4、不代理/缓存/转码视频、不注入脚本改版。
- 外部官方页面的播放行为不受本 App 完整控制，Stage 21 不把其播放进度自动转成 `VIEWED/MASTERED` 或奖励。只有未来获得官方 SDK/API 或可验证回调后，才扩展可信完成证据。
- 家长可配置免费教育资源来源，但服务端只接受无用户名/密码、无查询/片段、默认 443 端口的公共 HTTPS 首页；每次解析前校验 DNS 结果，拒绝 loopback、私网、链路本地、组播、保留地址和 IP 字面量，重定向逐跳复验。
- 栏目发现只读取最多 512 KiB 的 HTML，连接/读取总时限 8 秒、最多 3 次重定向、最多保存 30 个同源栏目；不执行页面 JavaScript，不提交表单、不携带 Cookie/认证、不读取视频或下载附件。失败保留最近一次成功快照并标为 `FAILED`，不清空孩子现有目录。
- 新来源和重新发现的栏目必须由 PARENT 批准后才进入儿童投影；CHILD 只获得来源名、栏目名、学段和同步时间，不获得来源/栏目 URL。幼儿园仍不显示外部资源目录。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP21-1 | 离线完成 | Spec、REQ-033–038、路线图、当前状态、TODO/BLOCKERS 和 V10/V11 数据模型 |
| WP21-2 | 离线完成 | Course/Unit/Lesson/Activity、九类活动、内容版本、发布与权利依据领域模型 |
| WP21-3 | 离线完成 | Assignment/Attempt/Completion/MasteryEvidence 状态机、幂等、复做与权限服务 |
| WP21-4 | 离线完成 | V11 JDBC Store、生产 REST API、OpenAPI 与 H2/PostgreSQL 权限/并发测试 |
| WP21-5 | 进行中 | Android 已有自主学习选择台、动态学习路径、九类活动投影、安装包视频 90% 实际播放、孩子提交和家长审核；持久化 outbox、进程重启、显式 401/409 冲突恢复和家长可视化课程编辑待建设 |
| WP21-5A | 离线完成 | V10 家长免费教育来源、受控 HTML 栏目发现、批准/撤回、儿童安全投影与 Android 来源书架；真实来源/平板回放待外部条件 |
| WP21-6 | 进行中 | V11 H2/PostgreSQL、Android 双变体和契约门禁已通过；Stage commit/push 与平板/E2E 限制继续回填 |

### V11 当前实施切片

V11 以一条可验收的生产纵切完成共用引擎，不在本轮提前制作 Stage 22–25 的大量课程内容：

1. 家长以嵌套草稿创建 `CourseVersion → Unit → Lesson → Activity`，课程固定归属家庭、学段和学科；发布记录权利说明、发布人和时间。已发布版本没有修改 API，后续改变必须新建版本。
2. 九类活动使用服务端固定完成规则。视频需要 `VIEWED`；选择、配对和排序需要服务端答案判定后的 `CHECKED`；亲子阅读、口头表达、线下实践和家长确认先记录 `ATTEMPTED`，最终由家长产生 `PARENT_CONFIRMED`。
3. Assignment 按课节分配并固定引用发布版本；孩子目录只返回自己的有效学段课节，绝不返回答案键、权利文档、原始来源 URL 或其他孩子记录。
4. 孩子尝试使用幂等键；满足孩子侧 required evidence 后才可提交。家长可把 `SUBMITTED` 确认为 `COMPLETED` 并追加 `MASTERED`，或要求复做；返工时间单独留痕，返工后没有新 Attempt 时服务端返回 409。
5. Android 只在现有“学习”入口增加服务端“学习路径条”，不新增一级导航。存在进行中课节时，普通家庭任务只进入“后来再做”，儿童端同一时刻只有一个主要行动；目标平板、离线 outbox 与完整冲突 UI继续按真实状态保留在 AC21-04/06。

V11 完成判定必须同时包含：领域状态向量、MockMvc 权限/幂等/答案隐藏测试、PostgreSQL V1–V11 schema validate、Android 两变体解析/渲染/构建，以及可解析 OpenAPI/证据；任一未运行项不标记通过。

## 数据、API、Android 与文档变化

- Flyway V10 先新增 `education_resource_source/category/action` 和索引，不修改 V1–V9；原计划的共用教学实体从 V11 连续新增，避免修改已经可能执行的迁移。预期教学实体仍包括 subject/course/course_version/course_unit/lesson/learning_activity/question/question_option/lesson_assignment/activity_attempt/learning_completion/mastery_evidence/content_action。
- API 采用 `/api/v1/families/{familyId}/teaching/...` 家长管理面和 `/children/{childId}/learning/...` 儿童执行面；所有写入需要幂等键，内容修改/完成审核需要版本检查。
- 来源 API 使用 `/education-resource-sources` 创建/查询、`/{sourceId}/refresh|approve|withdraw` 动作和 `/children/{childId}/education-resource-catalog` 安全投影；刷新动作不接收客户端返回的栏目结果，避免伪造发现内容。
- Android 在 Stage 20 的三入口和学段路由内渲染服务端活动；不增加儿童一级导航。离线队列只保存最小提交载荷并在重启后恢复，冲突时不静默覆盖服务端结果。
- 同步更新 OpenAPI、API 参考、数据库/架构设计、用户手册、Stage 账本和 `docs/evidence/stage-21/acceptance.json`。

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V21-01 | Java 领域 | 内容版本、活动校验、证据要求、完成/复做状态向量 | 非法类型/跳转/缺证据拒绝；已发布版本不被改写 | Stage 21 evidence |
| V21-02 | H2 MockMvc | 家长建课/发布/分配，孩子目录/尝试/提交，家长确认/复做，RBAC/跨家庭/幂等/版本冲突 | 200/201、400/401/403/404/409 和证据链准确 | Stage 21 evidence |
| V21-03 | PostgreSQL 16 | V1–V11、Hibernate validate、并发尝试/审核和全量后端测试 | 迁移、约束、唯一键、锁和状态机通过 | Stage 21 evidence |
| V21-04 | Android JVM/lint/build | 远端课程解析、学段过滤、活动提交、重启 outbox、401/409、debug/release | 自动化、lint 和两变体构建通过 | Stage 21 evidence |
| V21-04B | Java/H2/PostgreSQL/Android | 公共 HTTPS URL 策略、栏目发现上限/同源过滤、家长 RBAC、刷新/批准/撤回、失败保留快照、家长来源书架和儿童无 URL 投影 | SSRF 反向向量、栏目替换事务、动态 UI 状态和两变体门禁通过 | Stage 21 evidence |
| V21-05 | Android 平板 + 测试服务 | 四学段读取同一引擎不同投影，完成选择/视频/亲子/线下活动，断网重启恢复，家长复做/确认 | 真实 UI、网络、持久化、无障碍和 E2E 通过 | Stage 21 evidence/设备阻塞 |
| V21-06 | 通用 | diff、secret、Markdown 链接、证据 JSON、OpenAPI | 治理与机器契约一致 | Stage 21 evidence |

## 完成标准

- [x] AC21-01：`PASS_OFFLINE` — V11 只允许 DRAFT 发布且没有已发布内容修改/删除 API；新版本独立创建。孩子目录按家庭、本人、有效学段、PUBLISHED 和 Assignment 过滤，跨家庭/未发布拒绝。
- [x] AC21-02：`PASS_OFFLINE` — 九类活动由服务端枚举固定规则；安装包短视频累计实际播放至少 90% 才产生 VIEWED，四类客观活动服务端判题，四类亲子/口头/线下活动最终需要 PARENT_CONFIRMED；孩子响应无答案键。
- [x] AC21-03：`PASS_OFFLINE` — V11 的 Assignment/Attempt/Completion/MasteryEvidence 和 teaching_action 可回放；REWORK_REQUIRED 后必须新增 Attempt 才能重交，全部写入幂等，提交/审核使用 expectedVersion，PostgreSQL 并发 APPROVE/REWORK 仅一方成功。
- [ ] AC21-04：`PARTIAL` — Android 未新增一级导航，已能同步学习路径、提交活动/课节和家长确认/复做；但当前连接会话仍为内存态，尚未完成持久化 outbox、进程重启后的待提交恢复及专门的 401/409 冲突界面，不能标记通过。
- [x] AC21-04A：`PASS_OFFLINE` — 孩子可在现有“学习”入口切换“我的任务/自主学习”，按五级教材条件选择；官方页面只在受限 WebView 内手动打开，非官方顶层导航、文件访问、自动播放和视频地址抽取均被禁止。真实平板播放与平台后续页面路径仍归 AC21-06。
- [x] AC21-04B：`PASS_OFFLINE` — 家长可配置、刷新、批准和撤回免费教育来源；服务端安全发现同源栏目并动态替换快照，成功刷新回到待批准、失败保留旧快照；儿童只见匹配学段的已批准栏目标签且不含 URL。真实公共站点兼容和目标平板交互仍归 AC21-06。
- [x] AC21-05：`PASS_OFFLINE` — H2 46 项（6 项 PostgreSQL 条件跳过）、PostgreSQL 16.15 全量 46 项零失败/零跳过、V1–V11/55 表、Android debug/release 各 31 项、两变体 lint/assemble 和 OpenAPI/文档门禁通过。
- [ ] AC21-06：`BLOCKED` — 目标平板上的多活动交互、TalkBack、横竖屏、断网重启与家长/孩子端到端需要真实设备和测试服务。

## 安全检查、已知限制与交接

- 内容权利依据、发布人、版本和下架动作必须审计；儿童响应不包含权利文档、任意 URL、答案键或其他孩子数据。
- 题目答案只在服务端判定，Android 不下载可直接还原的答案键；自由文本做长度/日志脱敏，不做自动人格或心理标签推断。
- 自主学习入口只对小学、初中、高中开放，学段锁定为家长配置的 `effectiveStage`；幼儿园不进入外部网页。当前只对已核验的“一年级·语文·统编版·上册”使用用户提供的深链，其他组合安全回退官方同步课堂选择页，不猜测内部资源 ID。
- 2026-08-28 V11 在 V10 基础上新增 12 张课程/证据表；H2 全量 46 项通过（6 项 PostgreSQL 条件跳过），PostgreSQL 16.15 全量 46 项零失败/零跳过并验证 11 次迁移、55 张生产表和并发审核。Android debug/release 各 31 项 JVM、两变体 lint/assemble 已通过。结构化结果见 [Stage 21 evidence](../evidence/stage-21/acceptance.json)。这不替代真实公共来源兼容性、进程重启/outbox 和目标平板运行态。
- 公共 DNS 预检不能替代生产网络出口策略；家庭服务部署时仍应通过防火墙/出站代理禁止访问内网与云元数据地址。不同站点栏目结构不一致，JavaScript/登录/反爬站点可能安全失败，不以绕过站点控制提高成功率。
- Stage 20 因真机回放保持 `BLOCKED`，不阻止 Stage 21 的独立后端/自动化工作；两者可在同一设备窗口联合验收，但状态分别回填。
- Stage 22–25 只有在本引擎事实链稳定后才制作各学段深度页面和内容；Stage 26 负责连续性、隐私、平板和 Release 总验收。
- V11 Android 只播放 `lesson_color_garden`、`lesson_count_to_five`、`lesson_shape_home` 三个安装包内审核资源引用；未知 `contentRef` fail-closed。家长目前通过生产 API 建课/发布/分配，Android 可视化课程编辑器属于下一工作包。
