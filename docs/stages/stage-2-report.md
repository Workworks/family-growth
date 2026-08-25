# Stage 2：工程骨架与 Family / Growth 基础

状态：`BLOCKED`

日期：2026-08-25

产品 Phase：1

## 目标与非目标

目标：在可重复构建的 Spring Boot 模块化单体和 Android Compose 平板工程中，建立 Family、Parent、Child、GrowthPlan、GrowthGoal、GrowthTask 的领域模型、Flyway 迁移、基础用例与 API 契约，并交付能启动的家长/孩子双模式 Android 壳。

非目标：本 Stage 不实现任务提交/审核发奖、Wallet/Ledger、压岁钱、兑换、基金交易、防沉迷完整计时或真实登录。页面不得用固定成功冒充这些后续能力。

## 前置输入与环境基线

- Phase 0 设计与需求账本 REQ-001–011。
- Java 17.0.8、Maven 3.9.16；仓库将生成 Maven Wrapper。
- Android SDK 平台 34/36、Build Tools 34/35/36、API 34 模拟器；工程将生成 Gradle Wrapper，不依赖系统 Gradle 7.6.2 构建现代 Android 插件。
- V1 正式验收仍推荐 PostgreSQL；Stage 2 自动化可用 H2 的 PostgreSQL 兼容模式隔离测试。

## 边界与不变量

- 后端依赖 `domain <- application <- infrastructure/web <- boot`，领域层不依赖 Spring/JPA。
- 所有家庭对象必须归属 Family；跨 Family 查询不泄露对象存在性。
- Child 保存出生日期与年龄阶段，为后续适龄 UI/规则留边界。
- GrowthTask 包含 `LEARNING` 一级类别，但任务完成和奖励留到 Stage 3。
- 数据结构由 Flyway 建立，JPA `ddl-auto=validate`；API 路径 `/api/v1`。

## 工作包

| 工作包 | 状态 | 交付 |
| --- | --- | --- |
| WP2-1 工程基线 | 已完成 | Maven/Gradle Wrapper、五个后端模块、Android app |
| WP2-2 领域与用例 | 已完成 | Family/Parent/Child/Plan/Goal/Task 与归属校验 |
| WP2-3 持久化/API | 已完成 | Flyway、JPA adapter、REST 与 PostgreSQL 16 真实验证通过；认证属于下一 Stage |
| WP2-4 Android 壳 | 阻塞 | Compose 平板响应式 PARENT/CHILD 壳构建通过；Emulator 37.1.11 安装启动阻塞 |
| WP2-5 门禁证据 | 阻塞 | 后端、PostgreSQL、Android 自动化通过；Android 运行态未通过 |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V2-01 | Java 17 / H2 | Maven 全模块测试 | 领域、用例、迁移与 API 测试通过 | `evidence/stage-2/acceptance.json` |
| V2-02 | PostgreSQL | 运行 Flyway + Hibernate validate | 迁移成功、映射一致 | 后续本 Stage 证据；缺环境不得以 H2 替代 |
| V2-03 | Android SDK 34 | `assembleDebug`、lint、单测 | 生成 APK 且门禁通过 | Stage 2 证据与 APK SHA-256 |
| V2-04 | API 集成 | 创建 Family/Parent/Child/Plan/Goal/Task 并做权限反向 | 正向成功、跨家庭/孩子写操作失败 | Spring 集成测试 |
| V2-05 | 模拟器/平板 | 安装启动并旋转 | 双模式壳可启动，横竖屏无崩溃 | 运行态截图/日志；未执行不得 PASS |

## 完成标准

- [x] AC2-01 Maven 与 Gradle Wrapper 可执行。
- [x] AC2-02 后端模块依赖与领域模型满足架构边界。
- [x] AC2-03 Flyway 与 JPA validate 在 PostgreSQL 16.15 通过。
- [x] AC2-04 Family/Parent/Child/Plan/Goal/Task API 正向、校验和跨家庭反向自动化通过；正式认证仍属后续安全工作。
- [ ] AC2-05 Android debug APK 构建成功，平板双模式壳不冒充后续功能。
- [x] AC2-06 当前文档、测试、命令、退出码、产物哈希和限制已回填。

## 安全与限制

本 Stage 不实现认证时，不得将无鉴权 API 描述为可部署产品；集成测试可使用显式测试身份头，生产 profile 必须 fail-closed 或保持未开放。不得存储明文 PIN、secret 或孩子图片。

## 执行记录

- 首次 `mvn -N wrapper:wrapper -Dmaven=3.9.16` 生成 Wrapper，但 PowerShell 将版本值错误传成 `3`，随后 `.\\mvnw.cmd test` 在项目编译前因未知生命周期 `.9.16` 退出 1。已直接修正 Wrapper distribution URL；该结果不计为产品测试失败，也不记为业务 Bug。
- 系统 Gradle 7.6.2 执行 Android `wrapper` 时先加载 AGP 8.7.3，因最低要求 Gradle 8.9 在源码编译前退出 1。采用独立 `wrapper-bootstrap.gradle` 生成 8.9 Wrapper 后再运行正式构建。
- 自举脚本首版重复注册 Gradle 内建 `wrapper` 任务，配置阶段退出 1；已改为 `tasks.named` 配置既有任务。以上两项均是构建自举命令问题，尚未构成 Android 源码测试结论。
- Gradle 8.9 首次正式门禁在 `checkDebugAarMetadata` 发现 Compose 1.8.2 / Activity 1.10.1 要求 compileSdk 至少 35，工程初值 34，源码尚未编译即退出 1。本机已有 Android SDK 36，故 compileSdk 调整为 36，targetSdk 仍保持 34；该兼容选择需在后续依赖基线评审中固定。
- Android `testDebugUnitTest lintDebug assembleDebug` 重试退出 0，53 个任务完成并生成 debug APK。AGP 8.7.3 仅声明测试到 compileSdk 35，对 36 发出警告；构建通过但在升级 AGP 或安装 SDK 35 前保留为已知限制。
- Maven 最终门禁退出 0：六模块成功，领域 3 项与 Spring/Flyway/JPA/API 3 项通过。API 测试覆盖 Family→Child→Plan→Goal→LEARNING Task、非法任务时长 400 与跨家庭归属 404。
- APK 大小 10,073,090 bytes，SHA-256 `F13B893706C8DCBC32E0724FA75BB7D4025C206B5C196BB7B4EFE38A02F136CD`。
- `Stage118_API_34` 模拟器启动后持续处于 ADB offline；首次等待 60 秒超时，重连再等 40 秒仍失败，故 APK 安装与 Activity 启动均为 `NOT_RUN`。本次启动的两个模拟器进程已停止，Stage 不得标记完成。
- Docker Server 29.6.1 可用，但本机无 `psql` 且没有缓存 `postgres:16-alpine` 镜像。本轮没有未经记录地拉取镜像或创建持久卷，PostgreSQL 迁移门禁保留为下一动作。
- 用户确认继续后，新增 `flyway-database-postgresql` 运行时支持和只在 `STAGE2_POSTGRES_URL` 存在时执行的 PostgreSQL 结构测试。测试容器约束为：PostgreSQL 16、仅绑定 `127.0.0.1`、trust 认证仅供本机临时验收、不挂载卷、结束即删除。
- PostgreSQL 16 Alpine 镜像拉取成功。首次定向测试的容器已就绪，但 PowerShell 将 `-Dsurefire.failIfNoSpecifiedTests=false` 拆成无效 Maven 生命周期，编译前退出 1；`finally` 成功停止并删除容器。该命令错误不计为产品测试失败，改为在 PostgreSQL 环境变量存在时运行无附加参数的全模块测试。
- PostgreSQL 重试退出 0：Flyway 在 PostgreSQL 16.15 迁移至 V1，Hibernate `ddl-auto=validate` 成功，测试确认 1 条成功迁移和 6 张核心表；本轮全模块领域 3 项、Boot/H2/API 3 项、PostgreSQL 1 项通过。容器使用 `--rm` 且未挂卷，验证后无残留。
- Android 进一步诊断显示两个既有 AVD offline 的共同原因是磁盘容量：新建的专用 Pixel Tablet AVD verbose 启动明确报错，需要 12 GB userdata，而 C 盘仅约 3.78 GB 可用。为不清除已有 AVD 数据，仅把全新 `FamilyGrowth_Stage2_API34` 的数据分区从默认 10 GB 调整为 2 GB后重试。
- C 盘容量问题消除后，专用 AVD 重建到 `D:\AndroidAvd\FamilyGrowth`（约 202 GB 可用），不再报空间不足。Emulator 37.1.11 在 `software`/SwiftShader、WHPX/关闭加速组合下仍在 ADB 可用前退出或保持 offline；现有两个用户 AVD未清除。APK 安装、Activity 启动、旋转和截图均为 `NOT_RUN`，因此 Stage 状态改为 `BLOCKED`。

## 阻塞解除条件

满足任一条件后恢复验收：

1. 修复或安装可正常启动的 Android Emulator 版本，并在项目专用 API 34 AVD 回放安装/启动/旋转；或
2. 连接启用 USB 调试的 Android 8+ 真机/平板，授权 ADB 后执行同一验收。

修改全局 Android SDK/Emulator 版本会影响其他项目，未获得用户明确授权前不执行。

2026-08-25 用户已明确回复“继续”，授权解除该环境阻塞。修复边界限定为重装 `emulator` SDK 工具包；已有 AVD、system image、platform/build-tools 和项目文件不删除。若官方仓库仅提供同一版本，则先执行同版本完整重装和自检，不从非官方来源安装历史包。

官方 SDK 仓库重装已完成：卸载前后均为 Emulator 37.1.11，`emulator-check accel` 报告 WHPX 10.0.22624 可用。重装未删除任何 AVD、system image、platform 或 build-tools。

重装后重新创建 D 盘项目 AVD，并尝试 headless/隐藏窗口、software renderer 与 WHPX；QEMU 进程可出现，但未建立 emulator console/ADB，随后退出或保持空转。同版本官方重装修复无效。下一步需要重装共享的 API 34 `system-images;android-34;google_apis;x86_64`（现有 AVD也依赖它），或连接真实 Android 设备。因前者会影响其他 AVD且超出“只重装 emulator 工具包”的既定边界，Stage 恢复 `BLOCKED`。
