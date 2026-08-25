# 项目行为日志

本文件记录影响项目范围、架构、环境、实现、验证或状态的关键行为。纯读取不逐文件流水记录，但读取目的和结论必须落入对应 Stage；命令级证据进入 `docs/evidence/stage-N/`。

## 2026-08-25

| 时间/顺序 | Stage | 行为 | 结果/决策 | 文档或证据 |
| --- | ---: | --- | --- | --- |
| 1 | 1 | 参考 `capital-agent-system` 初始化仓库治理和 Phase 0 | 建立 Git `main`、AGENTS、八份设计、Stage/TODO/证据体系 | `stage-1-report.md` |
| 2 | 1 | 补充学习、平板、防沉迷、今日使用与零钱回收定位 | 纳入 V1；基金与费用保持家庭内部、纯模拟、透明 | `requirements/requirement-ledger.md` REQ-002–009 |
| 3 | 1 | 建立需求永久登记规则 | 新增追加式需求账本，P0–P3 与固定状态 | REQ-010 |
| 4 | 2 | 用户授权按完整需求开始迭代，要求所有行为入 docs | Stage 2 启动；关键行为进本日志，命令结果进 Stage 证据 | REQ-011、`stage-2-report.md` |
| 5 | 2 | 检查本机构建环境 | Java 17.0.8、Maven 3.9.16、Gradle 7.6.2；Android SDK 34/36、Build Tools 34/35/36、两个 API 34 AVD 可用 | `stage-2-report.md` 环境基线 |
| 6 | 2 | 生成 Maven Wrapper 并首次运行测试 | Wrapper 插件生成成功，但 PowerShell 将 `-Dmaven=3.9.16` 解析成错误版本 `3`，首次测试在编译前以 `.9.16` 未知生命周期失败；修正 `distributionUrl` 后重试 | `stage-2-report.md` 已知执行记录 |
| 7 | 2 | 首次生成 Android Gradle Wrapper | 系统 Gradle 7.6.2 在执行 wrapper 任务前加载 AGP 8.7.3，被最低 Gradle 8.9 门禁拒绝；改用不加载 Android 工程的临时自举脚本 | `stage-2-report.md` 执行记录 |
| 8 | 2 | 运行首版 Wrapper 自举脚本 | Gradle 已内建 `wrapper` 任务，脚本错误地重复注册同名任务，配置阶段退出 1；改为配置既有任务后重试 | `stage-2-report.md` 执行记录 |
| 9 | 2 | 首次 Android 正式门禁 | Gradle 8.9 自举成功；Compose BOM 解析到 1.8.2，要求 compileSdk ≥35，而工程初值为 34，AAR 元数据检查在源码编译前失败；本机已有 SDK 36，调整 compileSdk=36，targetSdk 保持 34 后重试 | `stage-2-report.md` 执行记录 |
| 10 | 2 | Android 第二次正式门禁 | `testDebugUnitTest lintDebug assembleDebug` 退出 0，53 个任务成功；生成 debug APK。AGP 8.7.3 对 compileSdk 36 给出“仅测试到 35”警告，记录为版本基线限制 | Stage 2 证据待汇总 |
| 11 | 2 | 后端最终自动化门禁 | Maven 六模块退出 0；领域 3 项、Spring/Flyway/JPA/API 3 项通过，包含创建完整 Growth 链、非法时长和跨家庭 404 | `evidence/stage-2/acceptance.json` |
| 12 | 2 | Android 模拟器安装验收 | `Stage118_API_34` 启动后 ADB `emulator-5554` 持续 offline；首次 60 秒超时，重连 40 秒仍无设备，未执行安装/启动。停止本次启动的 emulator 与 qemu 两进程 | `evidence/stage-2/acceptance.json` |
| 13 | 2 | 检查 PostgreSQL 验收前置 | Docker Server 29.6.1 可用；本机无 `psql`，且无缓存的 `postgres:16-alpine` 镜像。本轮未擅自拉取镜像或创建持久卷，PostgreSQL 目标库门禁留待下一动作 | `stage-2-report.md` 限制 |
| 14 | 2 | 用户确认继续 Stage 2 | 复核 AGENTS、需求账本、Stage/TODO、Bug/AQ 和工作树；无用户既有代码冲突。决定使用仅绑定 localhost、无持久卷的临时 PostgreSQL 16 容器完成目标库门禁 | Stage 2 本轮证据 |
| 15 | 2 | 拉取 PostgreSQL 16 Alpine | 镜像拉取成功，digest `sha256:cf78e766...20685` | Stage 2 本轮证据 |
| 16 | 2 | 首次 PostgreSQL 定向测试 | 临时容器就绪，但 PowerShell 将 `-Dsurefire.failIfNoSpecifiedTests=false` 拆为无效生命周期，Maven 在编译前退出 1；`finally` 已停止并由 `--rm` 删除容器。改用无附加参数的全模块测试 | Stage 2 执行记录 |
| 17 | 2 | PostgreSQL 真实门禁重试 | PostgreSQL 16.15 上 Flyway V1、Hibernate validate、6 表断言和全模块测试通过；Maven 退出 0。临时容器已停止并删除，无容器和数据卷残留 | `evidence/stage-2/acceptance.json` 待更新 |
| 18 | 2 | 诊断 Android AVD offline | 两个既有 AVD 均 offline；为避免清除用户数据，新建专用 `FamilyGrowth_Stage2_API34` 平板 AVD。verbose 启动确认根因是默认 userdata 需要 12 GB，而 C 盘仅约 3.78 GB 可用。仅将新 AVD 数据分区改为 2 GB，不修改已有 AVD | Stage 2 Android 运行态证据 |
| 19 | 2 | 将项目 AVD 迁至 D 盘继续诊断 | 删除本轮创建且未启动成功的 C 盘专用 AVD，重建到 `D:\AndroidAvd\FamilyGrowth`；空间问题消除。使用 Emulator 37.1.11 支持的 `software` 渲染和 WHPX/关闭加速组合，QEMU 仍在 ADB 可用前退出或 offline | `evidence/stage-2/acceptance.json` |
| 20 | 1/2 | 阶段状态同步 | 用户连续确认进入工程迭代，Stage 1 收口 `COMPLETED`；Stage 2 后端/PostgreSQL/自动化完成，但 Android 运行态缺证据，改为 `BLOCKED` | Stage 1/2、roadmap、TODO、current |
| 21 | 2 | 用户授权修复全局 Emulator | Stage 2 恢复 `IN_PROGRESS`。限定只重装 SDK `emulator` 包，保留已有 AVD、系统镜像和其他 SDK 组件；官方仓库若只提供 37.1.11，则执行同版本完整重装 | REQ-012、Stage 2 |
| 22 | 2 | 重装 Android Emulator | 官方 SDK 仓库完整卸载并重装 `emulator` 包成功；前后均为 37.1.11，WHPX 10.0.22624 自检可用。已有 AVD、system image、platform/build-tools 未删除 | Stage 2 运行态修复证据 |
| 23 | 2 | 重装后回放项目 AVD | 重建 D 盘 API 34 Pixel Tablet AVD，分别尝试 headless 和隐藏窗口、software renderer 与 WHPX。QEMU 可启动但未建立 emulator console/ADB，随后退出或空转；APK 安装/启动仍 `NOT_RUN` | `evidence/stage-2/acceptance.json` |
| 24 | 2 | 环境阻塞重新收敛 | Emulator 工具包已按授权修复但问题仍在。下一步需重装共享 API 34 system image（会影响现有 AVD）或连接真机，超出本轮仅重装 emulator 包的授权边界，Stage 2 恢复 `BLOCKED` | Stage 2、TODO、current |
| 25 | 11 | 用户提出 GitHub Release 热更新和 APK 交付 | 登记 REQ-013，创建计划外 Stage 11。当前仓库无 GitHub remote，更新仓库改为构建配置；未配置时 fail-closed，不使用占位服务 | Stage 11 Spec |
| 26 | 11 | 实现 GitHub Release 更新客户端 | 完成 latest release、SemVer、精确 asset、HTTPS/仓库约束、250 MiB 上限、大小与 SHA-256 校验；失败删除缓存，不携带 Token | Stage 11、Android `update/` |
| 27 | 11 | 接入 Android 系统安装安全边界 | 新增 FileProvider 和 `REQUEST_INSTALL_PACKAGES`；首次跳转未知来源设置，授权后仅把已校验 APK 交给系统安装确认 | Stage 11、Android Manifest |
| 28 | 11 | 首次 Android 更新门禁 | 单测通过；`assembleDebug` 成功。lint 因 Lifecycle 2.9.1 detector 与 AGP 8.7.3 分析 API 不兼容崩溃；将 Lifecycle 对齐为 2.8.7 后重跑，不关闭检查器 | Stage 11 证据 |
| 29 | 11 | 完成发布端和最终门禁 | 新增稳定签名 secrets 驱动的 tag Release workflow；最终 `testDebugUnitTest lintDebug assembleDebug` 退出 0，0.1.1/versionCode 2 APK 的 v2 签名验证通过 | `evidence/stage-11/acceptance.json` |
| 30 | 11 | 收敛外部验收状态 | 本地 APK 已交付；仓库仍无 remote，Emulator 仍不可用，因此真实 Release 下载和同签名覆盖升级标记 `BLOCKED`，不以构建替代真机证据 | Stage 11、TODO、current |
| 31 | 12 | 用户要求严格对齐参考仓库治理结构 | 登记 REQ-014/P0，审计参考根入口、docs 一级分类、BLOCKERS、TODO、Stage、Spec 和 AGENTS。确认参考规范路径是 `docs/BLOCKERS.md`，不存在根 `BLOCK.md` | Stage 12 Spec |
| 32 | 12 | 重构 Agent 长期启动与工程规范 | AGENTS 对齐参考 Step 1–6，并增加需求永久登记、BLOCKERS、儿童隐私、账本、真实金融禁区、Android 更新和固定汇报要求；codex-skills 补齐 15 类长期约束 | `AGENTS.md`、`codex-skills.md` |
| 33 | 12 | 补齐参考 docs 分类与执行入口 | 新增 manuals/phases/research 及开发、测试、布局、部署、运维、验收、API 索引；docs 根文件与参考实际根一致，额外保留用户要求的 requirements 分类 | Stage 12 报告 |
| 34 | 12 | 建立文档与机器契约门禁 | 新增当前代码对应的 OpenAPI 3.1 和文档验证脚本；62 份 Markdown 断链、索引可达性和 Stage 状态通过，全部 evidence JSON/OpenAPI YAML 可解析，空白门禁通过 | `evidence/stage-12/acceptance.json` |
| 35 | 13 | 用户调整为 Stage 2–11 先宽后深并指定 UI 参考 | 登记 REQ-015–017，深度定制后移；新建 Stage 13 作为 Android 本地基础体验整合，不篡改 Stage 3–10 生产完成状态 | Stage 13 Spec |
| 36 | 13 | 审计 `E:\test\DiaperTracker` 前端 | 提取翡翠/琥珀/Slate、低阴影描边卡、首页总览、五区导航和分组设置；舍弃资产语汇与密集工具箱，形成 Family Growth 成长环视觉方案 | `design/35-family-growth-ui-breadth-baseline.md` |
| 37 | 13 | 建设本机基础业务内核 | 新增本地模型、JSON 私有持久化、纯业务引擎和 ViewModel；任务奖励、压岁钱、兑换、商店、储蓄、愿望、模拟基金和防沉迷均从空数据开始 | Stage 13、Android `core/` |
| 38 | 13 | 完成响应式 Compose 前端 | 建成今天/任务/钱包/成长/家长五区；宽屏常驻侧栏、窄屏底栏，采用成长环、翡翠/琥珀/Slate 和描边卡片；保留 Stage 11 更新入口 | Stage 13、Android `ui/` |
| 39 | 13 | 修正零钱回收权限闭环 | 静态复核发现孩子申请会立即扣款，不符合家长关键调账受 PIN 保护；改为待审申请，家长确认后才扣款并记录透明 2% 手续费 | Stage 13、`LocalFamilyEngineTest` |
| 40 | 13 | 执行 Android 最终自动化门禁 | `testDebugUnitTest lintDebug assembleDebug` 退出 0；10 项 JVM 测试 0 failure / 0 error，包含 5 项本地业务不变量测试 | `evidence/stage-13/acceptance.json` |
| 41 | 13 | 生成并核验 0.2.0 APK | 生成 versionCode 3 debug APK，aapt 包信息正确，v2 签名通过，SHA-256 已登记；未冒充 release 正式包 | `dist/family-growth-0.2.0-debug.apk`、Stage 13 证据 |
| 42 | 13 | 收敛运行态状态 | 可离线工程工作完成；因无可用 Android 设备，安装交互、计时和重启持久化未执行，Stage 13 标记 `BLOCKED` | Stage 13、BLOCKERS、TODO/current |
| 43 | 14 | 用户将热更新提升为最高优先级并授权公开仓库 | 登记 REQ-018/P0；GitHub CLI 已认证 `Workworks`，`Workworks/family-growth` 名称可用；创建 Stage 14，先审计再分 Stage commit 和公开推送 | Stage 14 Spec |
