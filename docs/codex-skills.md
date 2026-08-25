# 工程规范（Agent 长期约束）

所有 Coding Agent 的详细执行规则。启动流程见根目录 [AGENTS.md](../AGENTS.md)，当前阶段见 [stage-current.md](stage-current.md)，可执行任务账本见 [TODO.md](TODO.md)。本文件是规范正文，入口文件不重复全部内容。

## 0. 儿童最佳利益门禁（最高产品优先级）

- 儿童端直接用户下限为 3 岁；0–2 岁不提供儿童自主操作路径。
- 每个儿童页面必须通过“单一主要行动、最多三项一级导航、短句+图形、大触控目标、可预测反馈、随时暂停/交给家长”的审查。
- 3–5 岁任务默认需要成人支架；产品目标是促成现实行动和亲子回应，不是增加屏幕时长。
- 禁止连续签到、排行榜、变动/随机奖励、抽奖宝箱、倒计时压力、损失厌恶、羞辱、无限滚动、自动播放和诱导隐私放弃。行为科学只可用于支持理解、自主、休息、隐私和健康，不可用于操纵留存或消费。
- 奖励稳定、透明且与明确行动对应；反馈描述努力和过程，不以人格标签评价儿童。复杂钱包、费率、基金、NAV、盈亏和流水对 3–5 岁隐藏或转为家长共同操作。
- 儿童福祉与活跃、留存、时长、金融功能或商业指标冲突时，前者胜出；冲突需求必须记录到 AQ/需求账本并在 Spec 中说明取舍。
- Android 儿童端变更除常规门禁外，必须检查 3–5 岁路径的导航数量、金融术语暴露、操纵性模式、休息出口和家长协助入口。规范正文见 [设计基线](design/36-child-experience-behavioral-baseline.md)。

## 1. 事实源顺序

冲突时不得静默选择，按此优先级并在报告中说明取舍：

1. 用户当前明确指令；
2. 本文件与 [AGENTS.md](../AGENTS.md)；
3. [master-plan](program/master-plan.md)、[roadmap](program/roadmap.md)、[Phase→Stage 排期](program/phase-stage-plan.md)与[分期](phases/README.md)；
4. [需求账本](requirements/requirement-ledger.md)、[Bug](bug/bugList.md)、[AQ](aq/aq.md)和[阻塞清单](BLOCKERS.md)中的未完成项；
5. [Stage 路线图](stages/stage-roadmap.md)、[执行标准](stages/stage-execution-standard.md)和目标 Stage 报告；
6. 产品、架构、数据库、账本、Android、API、测试、部署、运维与用户手册。

`stage-current.md` 是上下文快照，`TODO.md` 是按优先级和责任组织的执行账本，二者都不是 Stage 状态的独立真相来源。每次任务必须读取二者，发现漂移时以 Stage 报告为准同步修复。

## 2. Token 与修改策略

- 最小读取：只读任务直接相关的文件与章节，不为了解全貌无边界通读。
- 最小修改：只改授权范围，不顺手重构无关代码，不覆盖用户已有变化。
- 最小但充分测试：按变更范围选择门禁，不得用低层测试代替高层闭环。
- 大日志、截图和二进制只保存必要产物；文档记录路径、大小、SHA-256 和摘要。

## 2a. Spec 驱动开发（强制）

执行细则见 [Spec 驱动开发指南](development/33-spec-driven-development.md)。新增功能、跨模块修复、界面重构、发布、部署或治理变化必须先有可执行 Spec；目标 Stage 已存在时并入对应报告，跨 Stage 契约放 `acceptance/`，架构方案放 `design/` 并由 Stage 引用。

实现前必须具备用户可观察目标、范围内/外和外部前置、安全不变量、分层验证方式及编号完成标准。实施只能在 Spec 边界内推进；完成时逐条回填 `PASS / FAIL / BLOCKED` 与证据。只读咨询、拼写和不改变行为的机械调整可复用现有约束。

## 2b. 需求永久登记（强制）

- 用户每条需求、建议、补充、否定意见、优先级和长期标准必须追加到 `requirements/requirement-ledger.md`。
- 原始意图忠实保存，解释与推断分栏记录；需求变化新增记录并关联旧项，历史不得删除。
- 优先级只用 `P0–P3`，状态只用账本规定集合。
- 任何交付未完成当轮需求登记、Stage 和证据回链，不算完成。

## 3. Bug、AQ 与阻塞

- Bug 唯一入口 [bugList](bug/bugList.md)，历史长期保留；修复需现象、复现、根因、变更与最小回归测试。
- 问答唯一入口 [AQ](aq/aq.md)，回答应指向真实代码、命令或正式手册。
- 外部阻塞和工程未完成统一在 [BLOCKERS](BLOCKERS.md)分类；详细状态仍以 Stage 报告为准。
- 测试/构建命令输入错误不是产品 Bug；缺环境或权限不得虚假标记修复。

## 4. 测试策略

| 范围 | 必需门禁 |
| --- | --- |
| Java 领域 | 领域单测、金额精度/舍入、不变量、非法状态转换 |
| Spring/API | 集成测试、Bean Validation、401/403/404 权限反向、冲突、幂等、并发 |
| 数据库 | Flyway、PostgreSQL 目标库、Hibernate `ddl-auto=validate`、约束和索引 |
| Android | JVM 单测、Compose UI 测试、lint、debug/release 构建、包名/版本/签名 |
| 安装/更新 | 真机或目标模拟器首装、横竖屏、同签名覆盖升级、数据保留、失败回滚 |
| E2E | 家长建任务→孩子提交→家长审核→三类奖励入账，以及兑换、模拟基金、线下兑现闭环 |
| 通用 | `git diff --check`、Markdown 本地链接、证据 JSON、OpenAPI YAML 解析 |

Mock 只用于隔离测试，不能替代安装态、局域网、PostgreSQL 或真机验收。自动化不得执行真实递归删除、force push、`DROP DATABASE`、停止系统服务或访问生产环境。

Maven 定向模块测试必须带 `-am`，避免依赖本地仓库旧构件：

```powershell
.\mvnw.cmd -pl family-growth-boot -am test
```

## 5. Git 策略

- 每个 Stage 通过门禁后独立 Conventional Commit；精准暂存，不混入无关 Stage 或用户修改。
- 提交前检查 `git status`、diff、空白和 secret；未提交不得写虚假 commit SHA。
- 禁止 `git reset --hard`、强制覆盖、丢弃用户改动和重写共享历史。
- 初始未提交工作树中必须明确事实，不得把全部未跟踪文件误称为本轮新增。

## 6. Stage、Phase 与状态

- Stage 状态只用 `NOT_STARTED / IN_PROGRESS / COMPLETED / BLOCKED`。
- 缺外部环境、凭据、仓库或设备用 `BLOCKED`；能力在范围内只是未做完用 `IN_PROGRESS`。
- `COMPLETED` 必须有真实产物、可回放命令、退出码、测试结果和证据。
- Phase 是产品生命周期，Stage 是可独立验收的工程包；映射以 `program/phase-stage-plan.md` 和 `phases/README.md` 为准。
- 新 Stage 使用下一个连续全局编号，并同步 roadmap、Stage 索引、docs 索引、current、TODO/BLOCKERS 和需求账本。

## 7. 命令与证据

关键命令记录：脱敏命令、Shell、工作目录、时间、退出码、耗时、结果摘要和产物 SHA-256/大小。证据存 `evidence/stage-N/`；JSON 至少含 `capturedAt`、`command`、`workingDirectory`、`shellType`、`exitCode`、`passed`。未运行使用 `NOT_RUN/BLOCKED`，不得推断为 PASS。

密码、Cookie、Authorization、Token、API Key、数据库凭据、签名私钥、儿童敏感资料不得进入证据、日志、Git 或回复。

## 8. 架构边界

- 依赖方向 `domain ← application ← infrastructure/web ← boot`；领域层不依赖 Spring/JPA/Web/Android。
- 模块化单体为 V1 基线，不无需求引入微服务、Kafka、Kubernetes；Redis 不是 V1 强依赖。
- DTO、持久化实体和领域模型分离；Controller 只负责协议、校验、认证授权和用例调用。
- Android 使用 Kotlin、Compose、Material 3、MVVM、Repository；PARENT/CHILD 共用一个 APK。
- 家庭、孩子、任务、钱包、订单和记录均执行服务端家庭/对象权限；前端隐藏按钮不构成授权。

## 9. 数据、账本与模拟投资

- Java 金额只用 `BigDecimal`；Money 数据库用 `NUMERIC(19,2)`，NAV/份额/费率按设计使用高精度定点数。
- Money/Coin 变化只能在同一事务追加 LedgerEntry 后更新余额；流水不可原地篡改。
- 兑换、买卖、赎回和线下兑现先生成费用预览，确认时校验规则版本并使用幂等键。
- Wallet、订单和持仓使用乐观锁或原子条件更新；并发和重放必须有反向测试。
- Flyway 只新增迁移，不修改已执行迁移；所有环境 JPA `ddl-auto=validate`；业务时间 UTC。
- 虚拟基金 NAV 必须可涨可跌，不使用真实基金代码、不接行情、不暗示收益保证。

## 10. 后端与 API

- Java 17、Spring Boot 3.x、Maven Wrapper、构造器注入、Java Time。
- API 基础路径 `/api/v1`，统一 `data/error/traceId`，Bean Validation，错误码稳定。
- 认证失效 401、角色禁止 403、未授权对象与不存在对象统一 404。
- 关键写操作需要鉴权、对象权限、幂等和审计；生产 profile 未实现认证时必须 fail-closed。
- API 变化同步 `reference/09-api-documentation.md` 与 `openapi.yaml`。

## 11. Android 与更新

- 平板横屏为首要 UI 基线，手机为兼容布局；每页必须有 loading、error、empty、权限和认证过期状态。
- 家长模式受 PIN 和服务端角色保护，孩子不得通过本地 UI 状态绕过。
- 防沉迷只声明已真实实现的 App 内时段/时长能力，不冒充系统级跨 App 管控。
- GitHub Release 更新仅接受配置仓库的 HTTPS 资产，校验 SemVer、固定文件名、大小、SHA-256、applicationId、versionCode 和同一签名。
- 安装必须交给 Android 系统确认；不静默安装、不绕过未知来源设置，正式发布使用稳定 release 证书。

## 12. 安全与儿童隐私

- PIN 使用 Argon2id 或 BCrypt，带限流和锁定；敏感字段、儿童图片、使用记录和日志最小化。
- 支持家长发起儿童数据导出与删除；保留边界在实现前明确。
- 路径先绝对化并验证允许目录；删除数据、证据或文件前确认精确范围、备份和恢复性。
- secret、签名私钥和真实金融凭据不进入 Git、日志、证据、APK BuildConfig 或回复。

## 13. 文档结构

`docs/` 的五个治理入口固定为 `README.md / BLOCKERS.md / codex-skills.md / stage-current.md / TODO.md`；`openapi.yaml` 是机器可读接口契约。其余新文档按用途进入 `acceptance/ aq/ bug/ design/ development/ evidence/ governance/ manuals/ operations/ phases/ program/ reference/ requirements/ research/ stages/`。

新增或移动文档后同步 `docs/README.md`。Markdown 使用 UTF-8 中文、清晰编号、可复制命令和必要表格；代码、API、配置、页面或部署变化时同步设计、OpenAPI、手册、Stage 与证据。

## 14. 测试数据与环境

- 产品启动不得自动插入展示型家庭、钱包或基金数据。
- 自动化 fixture 使用随机、隔离、可清理标识，不依赖会随时间失效的绝对时间。
- H2 只用于隔离测试，不替代 PostgreSQL 目标库；构建成功不替代 Android 安装和真机闭环。
- 不因镜像重建、环境变量变化或测试方便删除用户数据库卷、AVD 或本地数据。

## 15. 完成定义与汇报顺序

以下不算完成：只有接口/按钮无实现；固定成功或 Mock 页面；余额变化无流水；费用未预览；认证只靠前端；测试未执行或无退出码；APK 未安装却宣称可用；更新未做同签名覆盖升级；文档/OpenAPI/迁移与代码不一致。

汇报顺序：结论 → 已完成 → 未完成/阻塞 → Bug/AQ/需求更新 → 关键文件 → 测试结果 → 运行态 → 已知限制 → commit。最简格式仍须包含：

```text
完成:
修改:
验证:
下一步:
```
