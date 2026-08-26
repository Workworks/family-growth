# 用户需求与建议账本

本账本长期保留所有用户需求与建议。最后更新：2026-08-26。

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
| REQ-030 | 2026-08-26 | 用户当前产品反馈 | 目前不足以支撑全面的教学任务，要求把所有待做事项全部列出 | 将全面教学能力提升为当前 P0 产品主线；当前“通用任务 + 3 个内置视频”仅算基础样例。先完成课程/课节/活动/题库/评估/进度/内容发布/生产同步/适龄 UI 的全量缺口审计，再按 Spec 分 Stage 实现；真实金融等安全边界不变 | P0 | `ACCEPTED` | 待立项，建议 20–24 | `requirements/teaching-and-project-backlog.md`、`TODO.md` |

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
