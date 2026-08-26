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
| 44 | 14 | 审计公开范围并建立分 Stage Git 历史 | 141 个候选文件无大文件/凭据；APK、local.properties、keystore 和本地缓存均忽略；创建 Stage 2/11/13/12/14 五个 Conventional Commits | Stage 14 evidence 待汇总 |
| 45 | 14 | 使用 GitHub CLI 创建公开仓库 | `gh repo create Workworks/family-growth --public --source=. --remote=origin --push` 成功；PUBLIC 仓库 main 已推送 | `https://github.com/Workworks/family-growth` |
| 46 | 14 | 绑定 Android 默认更新源 | Gradle 默认 `GITHUB_REPOSITORY` 改为 `Workworks/family-growth`，仍允许构建参数/环境变量覆盖；不把 Token 写入 APK | Stage 14、发布手册 |
| 47 | 14 | 重建绑定仓库的 0.2.0 debug 基线 | 10 项 JVM 测试、lint、assemble、aapt、v2 debug 签名通过；BuildConfig 确认为 `Workworks/family-growth`，新哈希已登记 | Stage 14 evidence |
| 48 | 14 | 检查真实 Release 前置 | 仓库无四项 Android 签名 Secrets，latest Release API 返回 404；不发布 debug 包冒充正式更新，Stage 14 收敛为稳定签名/备份和设备阻塞 | Stage 14、BLOCKERS |
| 49 | 14 | 用户授权生成新的 Android release 签名 | 登记 REQ-019/P0；私钥备份目标为 `E:\FamilyGrowthSigningBackup`，恢复密码与私钥分离保存并限制访问；Stage 14 恢复 `IN_PROGRESS` | Stage 14、REQ-019 |
| 50 | 14 | 生成并验证稳定 release 身份 | 生成 4096-bit RSA/JKS，E 盘私钥与 C 盘恢复记录分离，目录 ACL 仅当前身份 FullControl；配置四项 GitHub Secrets；`testReleaseUnitTest lintRelease assembleRelease` 与 v2 证书指纹通过 | Stage 14 evidence |
| 51 | 14 | 发布稳定签名 v0.2.0 基线 | tag workflow `32833035700` 成功；公开 Release 精确资产、GitHub digest、versionCode 3 和 release 证书下载复验通过，本地保存到 `dist/` | v0.2.0 Release、Stage 14 evidence |
| 52 | 14 | 发布同签名 v0.2.1 更新 | 源码递增到 versionCode 4 并独立 commit/tag；workflow `32833403426` 成功；latest API、digest、版本和同证书下载复验通过 | v0.2.1 Release、Stage 14 evidence |
| 53 | 14 | 对齐 GitHub Actions 运行时 | CI 成功但提示旧 Node 运行时弃用；从官方 release API 确认后将 checkout/setup-java/gradle actions 升级到 v7/v6/v6 | `.github/workflows/android-release.yml` |
| 54 | 14 | 收敛真实更新状态 | 公开仓库、签名、Secrets、两版 Release 和远端契约全部完成；只缺 Android 设备上的系统确认与数据保留，Stage 14 标记 `BLOCKED` | Stage 14、BLOCKERS/TODO/current |
| 55 | 14 | 执行发布链收尾门禁 | 文档链接与 207 个 JSON 文件通过；首次 OpenAPI 检查引用旧路径而中止，定位实际文件 `docs/openapi.yaml` 后重跑完整门禁 | Stage 14 evidence、活动日志 |
| 56 | 3–7 | 用户恢复生产后端闭环主线 | 登记 REQ-020/P0，REQ-016 改为被部分取代；按 Stage 3→7 顺序建立认证、任务奖励、账本、兑换、商店储蓄愿望和纯模拟基金 Spec | Stage 3–7 报告 |
| 57 | 3 | 实现生产认证与权限边界 | 新增 BCrypt PIN、五次失败/15 分钟锁定、仅存 SHA-256 的 12 小时 Bearer 会话、PARENT/CHILD RBAC 和家庭/孩子对象 404 | Stage 3、OpenAPI |
| 58 | 3 | 实现 Completion 与三奖励原子事务 | Flyway V2 新增 7 张生产表；孩子提交、家长审核、XP/Coin/Money、两条不可变流水与幂等记录在同事务完成 | Stage 3 code/evidence |
| 59 | 3 | 修正 JPA/JDBC 写入顺序 | 首次集成测试发现 JPA 延迟 flush 导致凭据/钱包外键先写；家长/孩子跨边界保存改为 saveAndFlush，失败整体回滚 | Stage 3 evidence |
| 60 | 3/4 | 完成 Stage 3 并启动 Stage 4 | H2 与 PostgreSQL 16.15 全 API 门禁通过；OpenAPI 3.1/11 paths 可解析；Stage 3 收口，Stage 4 转 IN_PROGRESS | Stage 3 evidence、Stage 4 Spec |
| 61 | 4 | 完成通用 Wallet/Ledger 闭环 | 新增家长正负调账、强制原因/幂等键、数据库唯一约束和对账 API；PostgreSQL 两笔并发 -7/10 仅一笔成功，余额 3 且对账通过 | Stage 4 evidence |
| 62 | 4/5 | 收口 Stage 4 并启动 Stage 5 | H2/PG、权限、超扣、重放、并发与对账门禁完成；Stage 5 转 IN_PROGRESS | Stage 4/5 报告 |
| 63 | 15 | 用户提升儿童端发展适龄原则为最高准则 | 登记 REQ-021/P0；儿童端从 3 岁起，儿童最佳利益优先于留存/时长/奖励/金融完整度，Stage 5 暂让位但保留真实状态 | AGENTS、codex-skills、Stage 15 |
| 64 | 15 | 完成权威依据与两遍设计 Spec | 基于 AAP、WHO、Harvard 与 ICO 形成三入口、单任务、亲子回应、低刺激和无操纵门禁；现有儿童端五区/三币/基金被判定为过载 | `design/36-child-experience-behavioral-baseline.md` |
| 65 | 15 | 完成 Compose 儿童端收敛 | CHILD 仅保留今天/小任务/我的；首页一个任务/按钮；成长页移除基金、NAV、流水和交易；新安装默认 20/10 分钟且不覆盖已有配置 | Stage 15、Android `ui/`/`core/` |
| 66 | 15 | 首次本地 release 恢复记录解析 | PowerShell 把 `$label:` 误判为变量名，Gradle 未启动且未输出 secret；改用 `${label}:` 后 release 门禁通过 | Stage 15 evidence 执行注记 |
| 67 | 15 | 首次 APK 工具定位 | `sdk.dir` 的转义路径解码错误，apksigner/aapt 未执行；改用已记录绝对 SDK 路径后 v2 签名、包名和版本通过 | Stage 15 evidence 执行注记 |
| 68 | 15 | 完成 0.2.2 本地门禁与交付包 | debug 53 tasks、release 60 tasks 均成功；两侧各 13 项 JVM 测试通过，稳定证书签名 0.2.2/versionCode 5 APK 已复制到 dist | `evidence/stage-15/acceptance.json` |
| 69 | 15 | 发布 v0.2.2 儿童适龄更新 | Stage commit `68456d8` 推送；workflow `32838297884` 用时 4m18s 全部成功并创建公开 Release。两次 60 秒观察窗到期和一次 API EOF 均未改变远端执行状态 | Stage 15 evidence、v0.2.2 Release |
| 70 | 15 | 下载复验公开 APK | latest API 返回 v0.2.2 和 digest；远端 asset 的 versionCode 5、包名、大小、SHA-256 与既有 release 证书通过，覆盖本地 dist 交付副本 | `evidence/stage-15/acceptance.json` |
| 71 | 5–10 | 用户要求直接完成 Stage 5–10 | 登记 REQ-022/P0；按账本依赖连续推进，仍保留每 Stage Spec/迁移/测试/证据/commit，缺真机不虚假完成 | Stage 5–10 报告 |
| 72 | 8–10 | 补齐后续 Stage 可执行 Spec | 新增 Android 生产接入、跨域报告、部署/发布/总验收的边界、不变量和编号完成标准 | Stage 8–10 报告 |
| 73 | 5 | 完成 GiftMoney 与双向兑换闭环 | 新增 Flyway V4、版本化规则、十分钟费用预览、教育声明、钱包锁、Money/Coin 同组双分录、幂等与权限 API | Stage 5 代码/OpenAPI |
| 74 | 5/6 | PostgreSQL 收口并启动 Stage 6 | H2 全量回归及 PostgreSQL 16.15 V1–V4/API/权限/规则漂移/余额回滚门禁通过；隔离容器已移除 | `evidence/stage-5/acceptance.json` |
| 75 | 6 | 完成 RewardShop、Saving 与 Wish | 新增 Flyway V5、家长审批后扣 Coin/库存、拒绝/取消零扣款、Wallet↔Saving 守恒转移、显式愿望分配与服务端权限 | Stage 6 代码/OpenAPI |
| 76 | 6/7 | PostgreSQL 收口并启动 Stage 7 | H2/PostgreSQL 16.15 全量门禁通过；并发双审核仅一笔成功，测试容器已移除 | `evidence/stage-6/acceptance.json` |
| 77 | 7 | 完成纯模拟基金闭环 | 新增 Flyway V6、NAV 正负变化、版本化费率、十分钟预览、买卖订单、加权成本持仓、费用和 P&L；3–5 岁 CHILD 交易服务端拒绝 | Stage 7 代码/OpenAPI |
| 78 | 7/8 | PostgreSQL 收口并启动 Stage 8 | H2/PostgreSQL 16.15 V1–V6、并发确认、NAV/规则漂移、超卖和账本门禁通过；测试容器已移除 | `evidence/stage-7/acceptance.json` |
| 79 | 8 | 完成 Android 生产接入可离线工作包 | 新增 HTTPS/开发私网 URL 策略、内存父/子 Token、401 清理、登录同步卡、任务提交/家长确认和后端 RBAC sync snapshot | Stage 8 Android/后端/OpenAPI |
| 80 | 8/9 | 收口离线门禁并转 Stage 9 | JVM、MockMvc、lintDebug、debug/release build 通过；无平板故 Stage 8 保持 BLOCKED，Stage 9 可离线推进 | `evidence/stage-8/acceptance.json` |
| 81 | 9 | 完成 App 内使用与家庭报告域 | 新增 V7 UsagePolicy/Event、家庭时区今日摘要、家长月报和 Wallet/Ledger 对账；不采集其他 App 或儿童通信 | Stage 9 代码/OpenAPI |
| 82 | 9 | 完成 Android 同步可靠性收口 | 断网时复用 UsageEvent 幂等键，刷新失败保留最后成功快照；当前进程终止前的待上传事件不承诺恢复并登记 LIM-011 | Android remote/core、已知限制 |
| 83 | 9/10 | PostgreSQL 收口并启动 Stage 10 | H2 23 项、PostgreSQL 16.15 23 项、Android 16 项 JVM + lint/build 全部通过；隔离容器已移除 | `evidence/stage-9/acceptance.json` |
| 84 | 10 | 完成生产部署 fail-closed | 新增 prod PostgreSQL/非空凭据/TLS guard、非 root 只读 Compose 镜像和 secret 排除；H2/空密码/禁用 TLS 反向测试通过 | Stage 10 boot/deploy |
| 85 | 10 | 完成真实备份恢复演练 | custom dump 恢复到新 `family_growth_restore_*` 数据库，Flyway 标记和探针一致；临时文件、容器、网络和卷精确清理 | `evidence/stage-10/acceptance.json` |
| 86 | 10 | 完成全量离线与 0.3.0 本地发布门禁 | H2/PG 各 25 项、Android debug/release 各 16 项、Docker build、稳定 v2 证书、包名/versionCode/SHA-256 通过 | `evidence/stage-10/acceptance.json` |
| 87 | 10 | 明确总验收未满足项 | 真机运行/覆盖升级外部阻塞；服务端兑现、自助数据权利、禁用时段/放行审计保持 PARTIAL，未用本机实现冒充生产能力 | Stage 10、LIM-012/013 |
| 88 | 10 | 发布 v0.3.0 稳定 APK | Stage commit `90de7df` 和 tag 推送；workflow `32916602740` 用时 4m17s 全部成功并创建公开 Release | Stage 10 evidence、v0.3.0 Release |
| 89 | 10 | 下载复验并转外部阻塞 | 远端 APK versionCode 6、0.3.0、11,210,187 字节、GitHub digest 和既有 release 证书通过，覆盖本地 dist；Stage 10 仅剩真机/目标服务回放 | `evidence/stage-10/acceptance.json` |

## 2026-08-26

| 时间/顺序 | Stage | 行为 | 结果/决策 | 文档或证据 |
| --- | ---: | --- | --- | --- |
| 1 | 16 | 用户要求继续下一阶段并设计儿童舒适 App 图标 | 登记 REQ-023/P0；Stage 16 限定为低刺激品牌图标、Android 全形态 Launcher 资源和稳定 v0.3.1，真实平板视觉继续独立验收 | `design/37-child-comfort-brand-icon.md`、`stages/stage-16-report.md` |
| 2 | 16 | 用户报告应用内更新提示“APK 下载或校验失败” | 登记 REQ-024/BUG-002，暂停图标接入并先修更新主链；真实 v0.3.0 资产 URL、大小、digest 和 CDN 重定向当前有效，修复聚焦弱网重试、超时、进度线程和错误分类，不放宽安全校验 | `bug/bugList.md`、Stage 16 |
| 3 | 16 | 完成 BUG-002 离线修复 | 下载改为 30 秒连接、120 秒读取、最多 3 次 IO 重试、identity 传输、进度节流/主线程发布和分类错误；验证错误立即失败，来源/大小/SHA-256 未放宽 | Android `update/`、`UpdateModelsTest` |
| 4 | 16 | 使用 imagegen 生成儿童舒适图标 | 生成透明“圆角书本托起新芽”母版；项目脚本缩入 adaptive 60% 安全区并派生五档 legacy、round、adaptive 与 monochrome 资源 | `branding/`、`scripts/generate_android_icons.py`、图标预览 |
| 5 | 16 | 执行 Android 全量和稳定签名门禁 | 112 项任务通过；首次稳定签名 clean 被 Windows lint-cache 锁阻断，正常停止 Gradle daemon 后重跑 61 项成功；19 项 release JVM、lint、v2 签名、v0.3.1/versionCode 7 和图标资源通过 | `evidence/stage-16/acceptance.json` |
| 6 | 16 | 发布并复验 v0.3.1 | commit `f487430` 与 tag 推送；workflow `32923187112` 用时 4m21s 成功。远端 APK 11,366,435 字节、GitHub digest、versionCode 7、包名和稳定证书通过；本地 dist 已替换为远端资产 | Stage 16 evidence、v0.3.1 Release |
| 7 | 16 | 收敛 Stage 状态 | 可离线更新修复、图标和 Release 全部完成；真实平板桌面与应用内升级缺设备，Stage 16 标记 `BLOCKED`，BUG-002 保持 `FIXED_PENDING_DEVICE` | Stage 16、BLOCKERS/TODO/current |
| 8 | 17 | 用户自行测试并要求 Agent 先继续下一步 | 登记 REQ-025/P0；按现有 LIM 顺序启动 Stage 17，优先建设服务端冻结式零钱回收，不等待平板且不接真实支付 | `stages/stage-17-report.md`、LIM-012 |
| 9 | 18 | 用户反馈双视角排版、奖励浏览和教学视频任务 | 登记 REQ-026–028/P0 与 BUG-003；Stage 18 暂时抢占 Stage 17 代码实施。教学内容限定原创离线、无广告/推荐/自动播放，观看 90% 只提交任务并由家长审核发奖 | `design/38-child-reward-video-interaction.md`、`stages/stage-18-report.md` |
| 10 | 17/18 | 按 Stage 保存 Spec 并完成 Android 实现 | Stage 17 Spec 以 `85f14a6` 独立提交；Stage 18 完成双段视角、奖励详情/兴趣、三段原创本地视频、真实播放累计和视频任务审核闭环 | Stage 17 commit、Stage 18 Android 代码 |
| 11 | 18 | 执行 Android 离线与稳定签名门禁 | clean 后 debug/release 各 12 项 JVM、lint、assemble 共 113 个任务通过；APK 为 0.3.2/versionCode 8、三段 raw 视频资源和既有 release 证书 | 待写入 Stage 18 evidence |
| 12 | 18 | 发布并复验 v0.3.2 | workflow `32944254891` 用时 4m39s 成功；远端 APK 11,437,471 字节、SHA-256 `bdda44e1...9b010`、versionCode 8、包名和稳定证书通过，远端资产覆盖本地 dist | `evidence/stage-18/acceptance.json`、v0.3.2 Release |
| 13 | 18/17 | 收敛状态并恢复后端主线 | Stage 18 可离线工程已完成，仅真实平板交互/覆盖升级阻塞；Stage 17 恢复为 Agent 下一主线，未丢失任何 Stage 17 待办 | current/TODO/BLOCKERS、Stage 17/18 |
| 14 | 19 | 用户真机反馈更新仍停在 0% | 登记 REQ-029/BUG-004 并再次暂停 Stage 17；旧客户端在首字节前最多静默阻塞 120 秒且无系统调度/备用入口。Stage 19 保留所有安全校验，改造系统下载和官方 API 回退 | `stages/stage-19-report.md`、BUG-004 |
| 15 | 19 | 完成系统下载与阶段反馈改造 | APK 交给 DownloadManager；排队/连接/暂停/下载/校验分开显示，45 秒无新数据切换同 Release Asset API，可取消并在两入口失败后打开 Release 页面；大小/SHA-256 不降级 | Android `update/`、更新面板 |
| 16 | 19 | 执行 v0.3.3 双变体门禁 | clean 后 debug/release 单测、lint、assemble 共 113 个任务通过；v0.3.3/versionCode 9 和既有稳定证书通过 | 待 Stage 19 evidence |
| 17 | 19 | 准备独立热更新验证目标 | v0.3.3 保留为必须手动安装一次的修复基线；仅递增版本为 v0.3.4/versionCode 10，供新版客户端真实检查、下载和覆盖升级 | Stage 19 |
| 18 | 19 | 发布并复验两版更新测试链 | v0.3.3 workflow `32949154719` 4m16s 成功，远端 SHA-256 `9dbb1e46...38b85`；v0.3.4 workflow `32949579505` 4m28s 成功，latest SHA-256 `a0e359fa...0bc3e`；两版同证书 | `evidence/stage-19/acceptance.json` |
| 19 | 19/17 | 收敛 Stage 19 并恢复 Stage 17 | 可离线修复、两版 Release 和远端复验完成；必须由用户手动安装 v0.3.3 后更新 v0.3.4，故 BUG-004/Stage 19 保持设备阻塞；Stage 17 再次恢复 | current/TODO/BLOCKERS、Stage 17/19 |
