# 用户需求与建议账本

本账本长期保留所有用户需求与建议。最后更新：2026-09-01。

| ID | 日期 | 来源 | 原始意图 | 解释与边界 | 优先级 | 状态 | 目标 Stage | 落地/验收 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 2026-08-25 | 初始 80 节需求 | 儿童成长培养、任务激励、家庭虚拟经济和财商模拟 Android App | 单家庭、一个 APK、PARENT/CHILD、Spring Boot；真实金融均排除 | P1 | `PLANNED` | 1–10 | `design/01`–`08`；AC-V1-01–14 |
| REQ-002 | 2026-08-25 | 用户初始定位补充 | 产品定位包括但不限于学习类 App | 原 V1 仅把学习作为通用任务类别且不做复杂课程平台；该深度边界已由 REQ-030 扩大，历史保留 | P1 | `SUPERSEDED` | 2、3、8、9 | `design/01`、AC-V1-15、REQ-030 |
| REQ-003 | 2026-08-25 | 用户初始定位补充 | 有防沉迷功能 | V1 管控本 App 的时段、每日/连续时长、休息和家长 PIN 放行；不宣称跨 App 控制 | P1 | `PLANNED` | 8、9 | `design/01`、`design/02`、AC-V1-17 |
| REQ-004 | 2026-08-25 | 用户初始定位补充 | 使用平板运行 | Android 平板横屏为首要 UI 和真机验收基线，手机为兼容布局 | P1 | `PLANNED` | 2、8、10 | `design/06`、AC-V1-18 |
| REQ-005 | 2026-08-25 | 用户初始定位补充 | 奖励模式和虚拟钱包，奖励后增加可体现的金钱 | XP/Coin/Money 三奖励；Money 属于家庭账本，可申请线下兑现 | P1 | `PLANNED` | 3、4、10 | `design/04`、AC-V1-03 |
| REQ-006 | 2026-08-25 | 用户初始定位补充 | 父母和孩子两端，父母查看孩子进度和今日使用情况 | 一个 APK 双角色；家长看任务进度、App 前台/学习时长、剩余时长和最近活动 | P1 | `PLANNED` | 8、9 | `design/06`、AC-V1-02/16 |
| REQ-007 | 2026-08-25 | 用户初始定位补充 | 压岁钱存入虚拟钱包，兑换比例默认 1:1 | GiftMoney 入 Money Ledger；家庭线下回收默认 1 Money=¥1，比例可配置 | P1 | `PLANNED` | 5、10 | `design/04`、AC-V1-04/19 |
| REQ-008 | 2026-08-25 | 用户初始定位补充 | 零钱回收计划、虚拟基金按波动盈亏增加零花钱 | 纯模拟 NAV 涨跌影响账本 Money；不接真实基金/行情 | P1 | `PLANNED` | 7、9、10 | `design/05`、AC-V1-07–09 |
| REQ-009 | 2026-08-25 | 用户初始定位补充 | 作为中间商赚取提现手续费 | 解释为家长配置的家庭教育手续费，确认前透明展示并进入流水；平台不收真实费用 | P1 | `PLANNED` | 4、7、10 | `design/04`、`design/05`、AC-V1-19 |
| REQ-010 | 2026-08-25 | 用户治理要求 | 每次需求和建议按不同优先级列入长期记忆或文档，不允许丢失 | 以仓库追加式需求账本作为长期事实源；每轮强制登记、分级、追踪，禁止删除 | P0 | `IN_PROGRESS` | 全部 Stage | `AGENTS.md`、`codex-skills.md`、本账本 |
| REQ-011 | 2026-08-25 | 用户迭代授权 | 按完整需求开始迭代，期间任何行为都要落入 docs | 立即启动 Stage 2；范围/决策/环境/命令/测试/限制分别写入行为日志、Stage 报告和证据 | P0 | `IN_PROGRESS` | 全部 Stage | `governance/activity-log.md`、`stages/stage-2-report.md` |
| REQ-012 | 2026-08-25 | 用户环境修复授权 | 继续处理 Stage 2 Android 运行态阻塞 | 明确授权修复全局 Android Emulator；只重装 emulator 工具包，不删除已有 AVD、系统镜像或其他 SDK 组件，完成后回放 APK | P0 | `IN_PROGRESS` | 2 | `stages/stage-2-report.md`、`evidence/stage-2/acceptance.json` |
| REQ-013 | 2026-08-25 | 用户发布需求 | 实现基于 GitHub Release 的热更新，并提供当前版本 APK | P1 发布能力；检查公开 latest release、下载 APK、校验 GitHub SHA-256 digest、由系统确认安装；当前仓库无 remote，真实发布/覆盖升级需后续配置 | P1 | `IN_PROGRESS` | 11 | `stages/stage-11-report.md`、`evidence/stage-11/acceptance.json` |
| REQ-014 | 2026-08-25 | 用户长期治理要求 | docs、BLOCK 等治理文档严格遵循 `capital-agent-system`，长期标准写入 AGENTS.md，每次对话都按该标准执行 | 以参考仓库当前真实结构为模板：根治理入口固定、`docs/BLOCKERS.md` 为阻塞唯一入口、TODO 与 Stage 分离、分类索引完整；业务内容按 Family Growth 适配，不复制无关产品规则 | P0 | `VALIDATED` | 12、全部后续 Stage | `AGENTS.md`、`docs/stages/stage-12-report.md`、`docs/evidence/stage-12/acceptance.json` |
| REQ-015 | 2026-08-25 | 用户迭代优先级调整 | 现在粗略完成 Stage 2–11 功能 | 先交付可操作、可本地持久化的 Android 基础体验宽度，覆盖任务/审核奖励、钱包、压岁钱、兑换、商店、储蓄、愿望、模拟基金、防沉迷、报告与更新入口；不因此把生产后端和真机验收标记完成 | P1 | `IN_PROGRESS` | 13（覆盖 2–11 基础体验） | `stages/stage-13-report.md`、`design/35-family-growth-ui-breadth-baseline.md` |
| REQ-016 | 2026-08-25 | 用户范围约束 | 更深度的定制功能暂时不做 | 当时将服务端生产闭环后移；现由 REQ-020 明确恢复 Stage 3–7，系统级跨 App 管控和非 V1 深度定制仍后移 | P2 | `SUPERSEDED` | Stage 3–10 后续深化 | `stages/stage-13-report.md`、REQ-020 |
| REQ-017 | 2026-08-25 | 用户 UI 参考 | 前端界面完善好，参考 `E:\test\DiaperTracker` | 借鉴其翡翠/琥珀/Slate 调色、低阴影描边卡片、五区导航、首页总览和分组设置；不复制资产管理内容，转译为成长环与平板优先 Compose 信息架构 | P1 | `IN_PROGRESS` | 13 | `design/35-family-growth-ui-breadth-baseline.md` |
| REQ-018 | 2026-08-25 | 用户发布优先级与 Git 治理要求 | 把热更新排到最高优先级；由 Agent 使用 GitHub CLI 创建公共仓库；代码按阶段 commit；缺少条件及时说明 | 热更新提升为当前 P0；目标仓库采用已登录账号下可用名 `Workworks/family-growth`；首次未提交工作树按 Stage 所属范围精准暂存，公开前做 secret/二进制审计；正式更新必须使用可长期备份的稳定 release 签名 | P0 | `IN_PROGRESS` | 14（延续 11） | `stages/stage-14-report.md`、`evidence/stage-14/acceptance.json` |
| REQ-019 | 2026-08-25 | 用户签名授权 | 授权生成新的 Android release 签名，备份到 `E:\FamilyGrowthSigningBackup` | 明确授权 Agent 新建长期 release keystore；私钥备份到指定 E 盘目录，恢复密码与私钥分离存放并限制为当前 Windows 用户访问；四项签名值只写 GitHub Secrets，不进入聊天、Git 或证据 | P0 | `VALIDATED` | 14 | `stages/stage-14-report.md`、`evidence/stage-14/acceptance.json` |
| REQ-020 | 2026-08-25 | 用户迭代指令 | 开始 Stage 3–7 生产后端闭环任务 | 恢复此前后移的生产后端主线，按 Stage 3→7 顺序建设认证/RBAC、任务审核奖励、Wallet/Ledger、压岁钱兑换、商店储蓄愿望和纯模拟基金；每 Stage 独立 Spec、迁移、测试、证据与 commit | P0 | `VALIDATED` | 3–7 | `stages/stage-3-report.md`–`stage-7-report.md`、对应 Stage evidence/commit |
| REQ-021 | 2026-08-25 | 用户最高行为准则 | 小孩端不要太复杂，从 3 岁开始使用；运用心理学和其他行为学改造，并列为最高行为准则 | 儿童最佳利益高于留存/时长/奖励/金融完整度；3–5 岁亲子共用、一次一个行动、最多三个一级入口、低刺激与可预测反馈；行为科学只用于支架、理解、休息和自主，禁止成瘾/操纵设计 | P0 | `IN_PROGRESS` | 15、全部后续 Stage | `AGENTS.md`、`codex-skills.md`、`design/36-child-experience-behavioral-baseline.md`、Stage 15 |
| REQ-022 | 2026-08-25 | 用户连续交付指令 | 直接完成 Stage 5–10 | 按 5→10 依赖顺序连续完成生产兑换、商店/储蓄/愿望、纯模拟基金、Android 生产接入、跨域报告和 V1 发布验收；每 Stage 仍需独立 Spec/迁移/测试/证据/commit，真实设备缺失的安装项不得伪造完成 | P0 | `IN_PROGRESS` | 5–10 | `stages/stage-5-report.md`–`stage-10-report.md` |
| REQ-023 | 2026-08-26 | 用户下一阶段与视觉要求 | 继续下阶段任务，并设计让儿童看起来舒服的 App 图标 | 启动 Stage 16；图标面向 3 岁起儿童，采用温和低刺激配色、清楚轮廓、无文字/金币/金融暗示/高唤醒元素，兼容 Android adaptive/legacy launcher，并随稳定签名 v0.3.1 发布；真机视觉与覆盖升级仍需设备验收 | P0 | `IN_PROGRESS` | 16 | `design/37-child-comfort-brand-icon.md`、`stages/stage-16-report.md` |
| REQ-024 | 2026-08-26 | 用户缺陷修复指令 | 修复应用更新时提示 APK 下载或校验失败 | 作为 Stage 16 首要 P0：真实 Release 校验仍 fail-closed；增强 GitHub/CDN 弱网下载重试、超时、线程安全进度和错误分类，不通过关闭摘要/大小/来源校验规避失败 | P0 | `IN_PROGRESS` | 16 | `bug/bugList.md` BUG-002、`stages/stage-16-report.md` |
| REQ-025 | 2026-08-26 | 用户连续推进指令 | 用户自行测试 v0.3.1，Agent 先进行下一步任务 | 启动 Stage 17，优先解除 LIM-012：建设服务端零钱回收规则、透明报价、申请、家长审批冻结、线下 PAID 扣账、拒绝/撤销释放、幂等和 PostgreSQL 并发闭环；不接真实支付 | P0 | `VALIDATED` | 17 | `stages/stage-17-report.md`、`evidence/stage-17/acceptance.json`、`design/04-wallet-ledger-design.md` |
| REQ-026 | 2026-08-26 | 用户测试反馈 | 调整家长和孩子视角按钮排版 | Stage 18 将角色切换改为清晰的双段式控件；手机和平板都保留大触控目标，进入家长视角仍必须通过 PIN，不用前端排版变化绕过权限 | P0 | `IN_PROGRESS` | 18 | `stages/stage-18-report.md`、BUG-003 |
| REQ-027 | 2026-08-26 | 用户儿童端交互需求 | 孩子端需要更多界面交互，可以在奖励商店浏览想要的东西 | 在“我的”入口内提供奖励卡片、详情和“我想要/先不选”；孩子只能表达兴趣，不直接扣 Coin 或兑换，家长仍负责配置和确认 | P0 | `IN_PROGRESS` | 18 | `design/38-child-reward-video-interaction.md`、`stages/stage-18-report.md` |
| REQ-028 | 2026-08-26 | 用户教学内容需求 | 接入教学视频，在应用里看完就算完成任务 | 首版使用原创、离线、无广告/推荐/追踪且不自动播放的短视频；累计实际播放达到 90% 后生成已提交任务，仍由家长审核发放固定透明奖励 | P0 | `IN_PROGRESS` | 18 | `design/38-child-reward-video-interaction.md`、`stages/stage-18-report.md` |
| REQ-029 | 2026-08-26 | 用户真机更新反馈 | 更新仍无法完成，界面停在“下载并校验 0%” | Stage 19 再次抢占 Stage 17：用 Android 系统下载服务承接 GitHub Release、显示排队/连接/暂停/校验阶段，首字节卡顿后切换官方 Asset API 备用入口；仍严格校验来源、大小、SHA-256、包名和签名 | P0 | `VALIDATED` | 19 | 用户确认 v0.3.3→v0.3.4 可正常更新；`bug/bugList.md` BUG-004、`stages/stage-19-report.md` |
| REQ-030 | 2026-08-26 | 用户当前产品反馈 | 目前不足以支撑全面的教学任务，要求把所有待做事项全部列出 | 将全面教学能力提升为当前 P0 产品主线；当前“通用任务 + 3 个内置视频”仅算基础样例。先完成课程/课节/活动/题库/评估/进度/内容发布/生产同步/适龄 UI 的全量缺口审计，再按 Spec 分 Stage 实现；真实金融等安全边界不变 | P0 | `IN_PROGRESS` | 20–26 | `requirements/teaching-and-project-backlog.md`、`stages/stage-20-report.md`、`TODO.md` |
| REQ-031 | 2026-08-26 | 用户教学分层与前端要求 | 教学任务按幼儿园、小学、初中、高中分开展示；家长可配置年龄并展示不同页面；幼儿园 UI 有趣、吸引注意力，按钮有夸张反馈和震动；内置适龄纪录片源；小学至高中由 Agent 设计 | 扩展 REQ-030：以家长配置年龄得到推荐学段并允许受保护覆盖，四学段使用不同页面结构、活动类型和反馈档案。幼儿园“夸张”解释为短促、明显、可预测的视觉/单次触觉确认，须可关闭并遵守 Reduced Motion，禁止持续强刺激。纪录片先建来源/版权/年龄审核目录；只内置原创或明确授权内容，其他仅提供家长批准的官方入口，不开放儿童网页和推荐流 | P0 | `IN_PROGRESS` | 20–26 | `AGENTS.md`、`design/39-age-stage-teaching-experience.md`、`requirements/teaching-and-project-backlog.md`、`stages/stage-20-report.md` |
| REQ-032 | 2026-08-26 | 用户连续推进指令 | 直接进行下一步 | 按已确认路线立即启动 Stage 20，不再等待产品范围确认；先完成学段事实源、家长配置/审计、分层反馈与页面路由、纪录片权利目录，再进行 Stage 21 共用教学引擎 | P0 | `IN_PROGRESS` | 20 | `stages/stage-20-report.md`、`evidence/stage-20/acceptance.json` |
| REQ-033 | 2026-08-28 | 用户连续推进指令 | 继续 | 不暂停当前交付：先按 Stage 20 Spec 完成最终审查、证据、状态收口和阶段提交；随后以独立 Spec 启动 Stage 21 共用教学引擎，不以 Stage 20 的平板阻塞冒充完成，也不跳过课程/活动/题库/证据模型的边界设计 | P0 | `IN_PROGRESS` | 20–21 | `stages/stage-20-report.md`、`evidence/stage-20/acceptance.json`、`stages/stage-21-report.md` |
| REQ-034 | 2026-08-28 | 用户自主学习与外部课程需求 | 除家长布置作业外，孩子可以自主学习；引入国家中小学智慧教育平台同步课堂，并按截图选择学段、年级、学科、版本和册次 | Stage 21 增加儿童“我的任务/自主学习”二级切换，不增加一级导航；使用教材路径选择台并打开家长认可的 `basic.smartedu.cn` 官方页面。视频由官方站点实时提供，不把外部播放时长自动当作本 App 完成或奖励证据 | P0 | `IN_PROGRESS` | 21 | `stages/stage-21-report.md`、`evidence/stage-21/acceptance.json`、Android `OfficialSelfLearningScreen`/`SmartEduOfficialSourceTest`；待平板回放 |
| REQ-035 | 2026-08-28 | 用户使用范围补充 | App 只供家庭自用，不发布；要求忽略平台“不得转载或改编”声明 | 接受“家庭自用、不发布”作为部署范围，但不将其解释为已取得转载/改编许可。采用受限 WebView 直接浏览官方页面，不下载、缓存、转码、抽取、代理或改编视频流；如未来要内置/离线分发，仍需平台许可或官方 SDK/API | P0 | `IN_PROGRESS` | 21 | `aq/aq.md`、`stages/stage-21-report.md`、`evidence/stage-21/acceptance.json`、官方网站声明核验记录；待平板回放 |
| REQ-036 | 2026-08-28 | 用户扩展免费教育来源需求 | 除已有来源外，家长端可配置免费教育资源来源网址，并读取相应分类栏目进行动态调整 | Stage 21 增加家长专属来源书架：只接受无凭据/查询/片段的公共 HTTPS 首页，服务端受限读取 HTML 导航栏目并保存快照，家长批准后动态投影给对应学段。儿童响应不含原始 URL，不能打开任意网页；成功刷新重新待批准，失败保留最近一次成功栏目和既有批准状态 | P0 | `IN_PROGRESS` | 21 | `stages/stage-21-report.md`、V10/API、Android `EducationResourceSourceUi`、`Stage21ResourceApiTest`、`evidence/stage-21/acceptance.json`；待真实来源/平板回放 |
| REQ-037 | 2026-08-28 | 用户连续推进指令 | 现在进行下一步 | 不等待 Stage 20/21 的外部平板条件，按当前 TODO 最高可执行顺位进入 Stage 21 V11：交付不可变课程版本、九类活动、课节分配、尝试/提交/家长复做或确认、最小学习证据链和 Android 动态课程入口；真实设备能力仍不得用构建结果代替 | P0 | `VALIDATED` | 21 | V11、生产 API、Android 学习路径；H2/PostgreSQL 16.15/Android 双变体门禁；`evidence/stage-21/acceptance.json` |
| REQ-038 | 2026-08-28 | 用户连续推进指令 | 继续 | 保持 REQ-037 的 V11 实施边界连续推进，完成动态视频可信播放入口、Android 学习路径/家长审核、契约和全量门禁；不因中途首轮测试通过提前停止或虚假收口 | P0 | `VALIDATED` | 21 | `LearningPathUi`、实际播放 90% VIEWED、家长确认/复做、OpenAPI 与全量门禁；平板仍归 AC21-06 |
| REQ-039 | 2026-08-28 | 用户连续推进指令 | 进行下一步 | 按 TODO 进入 Stage 21 V12：加密持久化最小学习 outbox，保留原幂等键并在重新连接后恢复；401 保留队列等待家长登录，409 刷新服务端事实后显式合并，不静默覆盖或丢弃。家长端增加“一课一活动”的可视化建课、发布与分配纵切；足量课程、多课节自由编排和目标平板仍不冒充完成 | P0 | `VALIDATED` | 21 | `LearningOutbox.kt`、`TeachingStudioUi.kt`、课程版本读取 API、Android 双变体 40 项与 Stage 21 evidence；真机运行态仍归 AC21-06 |
| REQ-040 | 2026-08-29 | 用户发布要求 | 本次完成后，发布新版本 | 在已完成 Stage 21 V12 后立即发布下一稳定版本：从 v0.3.4/10 递增为 v0.3.5/11，使用既有稳定 release 身份和公开 GitHub Release tag workflow；验证测试/lint/构建、tag/版本一致、GitHub digest、包名、版本与同证书。目标平板覆盖升级仍单独保留，不以远端 Release 冒充设备验收 | P0 | `VALIDATED` | 21 | [v0.3.5 Release](https://github.com/Workworks/family-growth/releases/tag/v0.3.5)、run 33190582209、SHA-256/包名/版本/稳定证书复验、Stage 21 evidence |
| REQ-041 | 2026-08-29 | 用户连续推进指令 | 继续下一步 | 按 TODO 最高顺位启动 Stage 22 幼儿园深度教学；先固化 3–5 岁亲子现实活动、单一行动、最少屏幕时间、原创内容权利和完成证据 Spec，再实现幼儿园专用安全投影。不得提前制作小学以上模板或把界面骨架冒充足量课程 | P0 | `IN_PROGRESS` | 22 | `stages/stage-22-report.md`、Android `KindergartenActivityPolicy`/幼儿园学习投影；完整内容与真机验收待后续 WP |
| REQ-042 | 2026-08-29 | 用户连续推进指令 | 开始下一步任务 | 按 Stage 22 WP22-3 实现服务端幼儿园内容发布规则：持久保存 3–4/5–6 岁内容带和健康/语言/社会/科学/艺术标签，并在发布时拒绝超时、活动过多、选择过多或缺少亲子/离屏活动的课程；小学以上契约不受影响 | P0 | `VALIDATED` | 22 | Flyway V12、`Stage21TeachingModels.validateForPublish`、H2/PostgreSQL 16.15 全量测试与 Stage 22 evidence |
| REQ-043 | 2026-08-29 | 用户连续推进指令 | 开始下一步任务 | 按 Stage 22 WP22-4/5 交付首批可审计原创幼儿园内容和家长闭环：10 个活动覆盖 2 个年龄带 × 5 领域，家长从单个陪伴折页预览/布置，并用一句具体行为观察回应；不引入外部媒体、奖励数字或成绩评价 | P0 | `IN_PROGRESS` | 22 | `stages/stage-22-report.md`、`design/40-kindergarten-original-parent-child-pack.md`、Android 内容目录/家长模板/测试与 Stage 22 evidence |
| REQ-044 | 2026-08-29 | 用户连续推进指令 | 继续下一步 | 按 TODO 启动 Stage 23“小学探索手册”：先建立低/高年级差异、主要学科、练习/阅读/实验、错题再练和小学报告 Spec，再实现 Android 单一当前探索与可解释事实状态首切；不把年龄当诊断或用同一页面换色冒充分层 | P1 | `IN_PROGRESS` | 23 | `stages/stage-23-report.md`、Android `PrimaryLearningPolicy`/探索夹页与 Stage 23 evidence |
| REQ-045 | 2026-08-30 | 用户连续推进与发布指令 | 实施下一步，并发布新版本；继续 | 完成 Stage 23 WP23-3：由服务端持久化小学低/高年级带，家长鉴权配置并保留不可变审计，切换不删除既有课程版本或 Attempt；门禁通过后以既有稳定签名发布 v0.3.6/12，并复验公开 APK 的摘要、包名、版本和证书。目标平板覆盖升级仍按真实设备单独验收 | P0 | `VALIDATED` | 23 | [v0.3.6 Release](https://github.com/Workworks/family-growth/releases/tag/v0.3.6)、run 33292966665、`stages/stage-23-report.md`、`evidence/stage-23/` |
| REQ-046 | 2026-08-30 | 用户教学与奖励补充 | 孩子视角需要更多自主学习内容；家长可直接更改学习阶段，孩子直接看到对应课程；认真看完课程提供家长可配置的预设奖励 | 复用 Stage 21 课程版本和完成证据：儿童自主目录仅投影当前服务端有效学段的已发布适龄课程；视频达到真实播放阈值且课程完成后才可按家长预设规则发放 Money/Coin/XP，服务端以幂等事务写奖励与 LedgerEntry，孩子不能改规则或伪造完成。首批内容必须原创或权利清晰，禁止自动播放/下一集 | P0 | `VALIDATED` | 23 | V14、Stage23 API/JDBC、Android 生产同步、H2/PostgreSQL 16.15/并发门禁、`evidence/stage-23/` |
| REQ-047 | 2026-08-30 | 用户连续推进指令 | 继续下一步 | 立即实施 REQ-046 的生产服务端闭环：家长配置每个孩子的固定自主学习奖励；孩子/家长同步时仅自动加入当前有效学段的已发布课节；奖励在分配时固化快照，只有真实证据完成并经家长批准后，才以同一数据库事务幂等增加 XP/Money/Coin 并写不可变 LedgerEntry。客户端刷新必须先安全同步自动课程，失败时保留既有事实 | P0 | `VALIDATED` | 23 | `stages/stage-23-report.md` WP23-9、V23-08/10、`evidence/stage-23/` |
| REQ-048 | 2026-08-30 | 用户连续推进指令 | 进入下一步；继续 | 按 Stage 23 当前顺位实施 WP23-4/5：提供小学低/高年级语文、数学、英语、科学首批原创短活动模板；把孩子“我没看懂”、客观题答错、家长错因归类、间隔再练和复做结果保存为不可变过程证据。求助不得算作尝试或完成，家长私密备注与答案不得投影给孩子 | P1 | `VALIDATED` | 23 | `stages/stage-23-report.md` WP23-4/5、V23-11/12、`evidence/stage-23/` |
| REQ-049 | 2026-08-30 | 用户项目管理查询 | 把目前排期计划的所有任务都列出来 | 以 TODO、Stage 20–26 路线、全面教学 backlog、产品深度 backlog、BLOCKERS 和 AQ 为事实源，列出全部未完成/部分完成事项；区分 Agent 可立即推进、计划 Stage、并行 P0、安全治理和必须由用户提供设备/决策的任务，不把历史已完成项重新列为待办 | P1 | `VALIDATED` | 23–26、并行治理 | 本轮汇总；`TODO.md`、`BLOCKERS.md`、`requirements/teaching-and-project-backlog.md` |
| REQ-050 | 2026-08-30 | 用户连续总交付授权 | 把上面的所有任务一次性完成，由 Agent 决策方向，不用逐项询问 | 作为跨 Stage 连续执行授权：Agent 自主决定普通、可逆且可验证的产品/技术默认值并记录依据；按儿童最佳利益、安全不变量和依赖顺序完成 Stage 23–26、数据权利、完整防沉迷、Android 生产接入与发布。授权不替代真机、第三方许可或部署环境，不扩大到侵权、真实金融、秘密泄露和破坏性操作 | P0 | `VALIDATED` | 23–26、并行治理 | Stage 23–26 报告/证据、v0.3.8 Release；目标平板/可信 HTTPS 按原授权边界保留阻塞 |
| REQ-051 | 2026-08-31 | 用户持续总交付指令 | 继续所有任务一次性完成，由 Agent 决策方向，不用过问 | 不把 Stage 26 的阶段收口误当项目总清单完成；重新审计 P-01–P-21，并按依赖连续建立 Stage 27–32。普通可逆默认由 Agent 决定；儿童最佳利益、隐私、账本、真实金融、第三方许可和真实设备边界不变 | P0 | `IN_PROGRESS` | 27–32、外部验收 | `requirements/teaching-and-project-backlog.md`、Stage 27–32、TODO/BLOCKERS |
| REQ-052 | 2026-09-01 | 用户再次确认连续总交付 | 把上面的所有任务一次性完成，并由 Agent 决策方向，不用过问 | 延续并强化 REQ-051：不因单个 Stage 自动化通过停下，按 Stage 29→32 依赖顺序连续完成所有 Agent 可执行工程、治理、验证和阶段提交。自主默认值必须记录依据；真实平板、可信外部部署、第三方授权及破坏性/越权行为仍不以“不用过问”替代真实条件或安全边界 | P0 | `IN_PROGRESS` | 29–32、外部验收 | Stage 29–32 报告、证据、分阶段提交与最终 TODO/BLOCKERS 审计 |

## 维护记录

- 2026-08-25：建立账本并回填项目初始需求、定位补充和需求治理要求。
- 2026-08-25：登记正式迭代授权与全行为文档化要求，启动 Stage 2。
- 2026-08-25：登记用户对全局 Android Emulator 修复的明确授权。
- 2026-08-25：登记 GitHub Release 热更新与当前 APK 交付需求。
- 2026-08-25：登记严格对齐 `capital-agent-system` 文档治理结构和每次对话执行协议的长期要求。
- 2026-08-25：登记先完成 Stage 2–11 基础体验宽度、深度定制后移及参考 DiaperTracker 完善前端的范围调整。
- 2026-08-25：登记热更新提升为 P0、Agent 创建公开 GitHub 仓库及按 Stage 提交的发布治理要求。
- 2026-08-25：登记生成新 Android release 签名并备份到 `E:\FamilyGrowthSigningBackup` 的明确授权。
- 2026-08-25：登记恢复 Stage 3–7 生产后端闭环，REQ-016 的整体后移约束由 REQ-020 部分取代。
- 2026-08-25：登记儿童端从 3 岁起、极简与发展适龄改造要求，并提升为不可被普通功能需求覆盖的最高产品行为准则。
- 2026-08-25：登记直接连续完成 Stage 5–10 的 P0 指令；允许 Agent 连续推进工程与发布，真机验收仍以真实设备为完成前提。
- 2026-08-26：登记继续下一阶段并设计儿童舒适 App 图标的要求，启动 Stage 16 品牌图标与 v0.3.1 交付。
- 2026-08-26：登记应用内更新“APK 下载或校验失败”缺陷，作为 Stage 16 首要 P0 修复且保持安全校验不降级。
- 2026-08-26：登记用户自行测试期间继续下一步工程，启动 Stage 17 服务端零钱回收生产闭环。
- 2026-08-26：登记测试反馈中的视角切换排版、奖励商店浏览和应用内教学视频完成任务三项 P0，启动 Stage 18；Stage 17 方案保留并暂时让位。
- 2026-08-26：登记 v0.3.1→v0.3.2 真机更新仍停在 0% 的二次反馈为 REQ-029/BUG-004，启动 Stage 19 并再次暂停 Stage 17 代码实施。
- 2026-08-26：用户确认 v0.3.3→v0.3.4 已可正常更新，REQ-029 验证完成、BUG-004 关闭；恢复 Stage 17 服务端零钱回收主线。
- 2026-08-26：Stage 17 在 H2/PostgreSQL 16.15 完成冻结式零钱回收、全局 available 保护、权限/幂等/并发门禁，REQ-025 验证完成、LIM-012 解除。
- 2026-08-26：用户再次要求“继续”；未扩大产品边界，按 REQ-025 完成 Stage 17 收尾、Android 契约兼容门禁和文档证据，后续新 Stage 仍需先满足 Spec 与隐私策略前置条件。
- 2026-08-26：用户明确指出现有能力不足以支撑全面教学任务；登记 REQ-030/P0，保留 REQ-002 历史并扩大其“仅通用学习任务”的旧边界，建立教学与全项目剩余事项清单。
- 2026-08-26：用户把全面教学进一步明确为幼儿园、小学、初中、高中四学段、家长配置年龄、分层页面与适龄纪录片来源；登记 REQ-031/P0，并在儿童最佳利益门禁下固化幼儿园明显但短促可关闭的视觉/触觉反馈边界。
- 2026-08-26：用户要求直接进行下一步；登记 REQ-032/P0，正式启动 Stage 20 学段底座、家长配置、分层路由与纪录片权利目录实现。
- 2026-08-26：Stage 20 可离线工程和全量门禁完成；REQ-030/031/032 继续保留 `IN_PROGRESS`，因为完整教学仍需 Stage 21–26，且 Stage 20 目标平板运行态验收尚未完成。
- 2026-08-28：用户要求继续；登记 REQ-033/P0，授权在 Stage 20 真实收口后直接进入 Stage 21 Spec 与共用教学引擎，不改变真机验收边界。
- 2026-08-28：登记 REQ-034/P0：孩子除家长任务外可自主学习，并按学段/年级/学科/版本/册次选择国家智慧教育平台同步课堂。
- 2026-08-28：登记 REQ-035/P0：产品仅家庭自用；保留用户要求忽略转载限制的原意，但工程采用官方页面受限 WebView，不复制或改编第三方视频流。
- 2026-08-28：REQ-034/035 完成 Android 离线纵切和两变体门禁；因目标平板官方页面/播放尚未回放，状态继续为 `IN_PROGRESS`，不得把网页入口标为教学引擎完成。
- 2026-08-28：登记 REQ-036/P0：允许家长配置免费教育资源网站并安全发现栏目；动态读取必须防 SSRF、限量、可回退和家长批准，儿童端不接收可启动 URL。
- 2026-08-28：REQ-036 的 V10、生产 API、H2/PostgreSQL 16.15、Android 来源书架和儿童安全投影通过离线门禁；因真实站点兼容和目标平板未回放，保持 `IN_PROGRESS`。
- 2026-08-28：登记 REQ-037/P0：按 Stage 21 下一工作包直接实施 V11 共用课程、九类活动、分配/尝试/提交/审核和 Android 动态课程纵切，不等待独立的真机阻塞项。
- 2026-08-28：登记 REQ-038/P0：用户要求不中断 V11，继续补齐视频内容引用、Android 学习路径和全部工程门禁后再报告。
- 2026-08-28：登记 REQ-039/P0：按既定下一动作启动 V12，加密持久化学习 outbox、重启/401/409 恢复及家长端“一课一活动”可视化建课纵切。
- 2026-08-28：REQ-039 完成离线工程验收：写前 AES/GCM 加密队列、原幂等键恢复、401 保留、409 显式合并和家长建课/发布/布置通过自动化；目标平板杀进程/密钥库回放仍作为外部验收保留。
- 2026-08-29：登记 REQ-040/P0：Stage 21 V12 完成后发布 v0.3.5/11，必须复用稳定签名并完成公开 Release 摘要、包名、版本和证书复验；设备覆盖升级证据继续独立保留。
- 2026-08-29：REQ-040 完成：v0.3.5/11 已由 tag workflow 使用稳定 release 证书发布为 GitHub latest，公开资产 digest、下载 SHA-256、大小、包名、版本与证书复验一致；真机 v0.3.4→v0.3.5 仍单独阻塞。
- 2026-08-29：登记 REQ-041/P0：按最高顺位启动 Stage 22，先交付幼儿园深度教学 Spec 与单一行动安全投影，足量原创内容、服务端内容带和目标平板验收继续真实追踪。
- 2026-08-29：登记并完成 REQ-042/P0：V12 持久化幼儿园年龄带/五领域标签，发布用例强制三活动、15 分钟总时长、8 分钟屏幕活动、两个选择和亲子/离屏活动门禁；H2/PostgreSQL 16.15 通过。
- 2026-08-29：登记 REQ-043/P0：继续 Stage 22 WP22-4/5，首包定量为 10 个原创亲子活动并完整覆盖 2 个年龄带 × 5 领域；家长端使用单个陪伴折页完成预览、布置和一句行为观察回应。
- 2026-08-29：REQ-043 完成可离线纵切和联合门禁：10 条内容目录、元数据建课契约、家长陪伴折页与观察回应已实现；目标平板仍未回放，需求保持 `IN_PROGRESS`，Stage 22 转为外部阻塞。
- 2026-08-29：登记 REQ-044/P1：按下一顺位启动 Stage 23；先以可验收 Spec 区分小学低/高年级，再实现不写虚假完成证据的 Android 探索夹页首切。
- 2026-08-29：REQ-044 完成 WP23-1/2 可离线首切：Android 低/高年级投影、真实状态和非完成型求助通过双变体门禁；服务端年级带、内容包、错因/再练和报告尚未完成，需求保持 `IN_PROGRESS`。
- 2026-08-30：登记 REQ-045/P0：继续实施 Stage 23 WP23-3，并在全量离线门禁通过后复用稳定 release 身份发布 v0.3.6/12；公开 Release 复验不能替代目标平板覆盖升级与数据保留验收。
- 2026-08-30：登记 REQ-046/P0：增加按服务端有效学段动态展示的儿童自主课程，并以真实观看/完成证据触发家长可配置的预设奖励；奖励必须在服务端账本事务中幂等入账，不降低儿童安全、内容权利或视频播放门禁。
- 2026-08-30：REQ-045 完成：v0.3.6/12 已由稳定证书发布；run 33292966665 成功，公开 APK digest、下载 SHA-256、大小、包名、版本和证书复验一致。目标平板覆盖升级仍单独阻塞；REQ-046 的服务端自动选课与奖励账本继续为 P0。
- 2026-08-30：登记 REQ-047/P0：按当前下一顺位实施服务端自动选课与奖励账本，不更换用户已批准的产品边界；默认奖励为零、分配时快照、家长批准后结算一次，Money/Coin 与 LedgerEntry 必须同事务。
- 2026-08-30：REQ-046/047 完成自动化验收：V14 奖励策略/审计与 Assignment 快照、显式最新同学段自动加入、批准后 XP/Wallet/Ledger 同事务结算以及 Android 生产同步通过 H2、PostgreSQL 16.15 并发和双变体门禁；目标平板与新 APK 发布仍独立保留。
- 2026-08-31：继续执行 REQ-050，不新增偏好询问。Stage 24/25 可离线范围已提交并推送；Stage 26 WP26-2–7 完成跨学段连续性、儿童数据权利、完整 App 内防沉迷和 Android 控制面，H2/PostgreSQL 16.15/Android 双变体通过。当前直接进入稳定 Release；目标平板、可信 HTTPS 和第三方许可边界仍不被连续授权替代。
- 2026-08-31：REQ-050 完成性审计推翻“只剩外部阻塞”的过早结论：P-13 Android 全生产 API、P-17 自动保留、P-18 单次会话/休息周期仍缺直接证据，均可由 Agent 实现。Stage 26 重新设为 `IN_PROGRESS` 并新增 WP26-10–13；已发布 v0.3.7 保留为中间里程碑，不回写成最终验收。
- 2026-08-31：REQ-050 的 Agent 可执行范围经二次完成性审计后 `VALIDATED`：P-13/P-17/P-18 已实现，H2/PostgreSQL 16.15/Android 双变体/文档契约通过，稳定签名 v0.3.8/14 由 workflow `33365966672` 发布并复验公开摘要、大小、包名、版本和证书。授权明确排除的目标平板、可信 HTTPS、第三方许可和外部备份运行态继续保留真实阻塞，不影响该连续工程授权的可执行范围验收。
- 2026-08-31：登记 REQ-051/P0。再次从原始总清单审计后确认 P-01–P-21 除 P-13/17/18 外仍有可实现深化，上一轮“当前没有可继续产品工作”的结论被证据推翻。已建立 Stage 27–32 依赖路线并启动 Stage 27；不会把目标平板阻塞扩散到可离线工程。
- 2026-08-31：REQ-051 推进到 Stage 28。Stage 27 的 P-01/02/03/12 已通过 H2、PostgreSQL 16.15、Android 双变体与文档门禁，可离线能力完成；目标平板相册/无障碍独立保持 `BLOCKED`，不暂停奖励治理建设。
- 2026-09-01：REQ-051/052 推进到 Stage 32。Stage 31 的 P-06/08/09/10/11 已通过 H2、PostgreSQL 16.15、Android 双变体与契约门禁；目标平板适龄隐藏和费用确认独立保持 `BLOCKED`，不暂停工具链与最终治理。
