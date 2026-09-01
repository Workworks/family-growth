# Family Growth 文档索引

本页是 `docs/` 的统一导航入口。五份治理入口固定在本目录：`README.md`、`BLOCKERS.md`、`codex-skills.md`、`stage-current.md`、`TODO.md`；`openapi.yaml` 是机器可读 API 契约。其余文档按用途进入子目录。

## 快速入口

| 我想… | 看这里 |
| --- | --- |
| 不知道从哪里开始 | [文档阅读指引](manuals/28-documentation-reading-guide.md) |
| 作为 Agent 接手 | [AGENTS](../AGENTS.md) → [工程规范](codex-skills.md) → [当前阶段](stage-current.md) → [TODO](TODO.md) → [Spec 指南](development/33-spec-driven-development.md) |
| 知道做到哪、还差什么 | [当前阶段](stage-current.md) → [Stage 路线图](stages/stage-roadmap.md) → [TODO](TODO.md) |
| 知道什么被阻塞 | [阻塞与限制清单](BLOCKERS.md) → [已知限制](acceptance/16-known-limitations.md) |
| 查全部需求与建议 | [需求账本](requirements/requirement-ledger.md) → [教学与项目剩余清单](requirements/teaching-and-project-backlog.md) → [登记规则](requirements/README.md) |
| 运行或开发项目 | [本地开发](development/17-local-development.md) → [开发指南](development/11-development-guide.md) → [测试计划](development/12-test-plan.md) |
| 开发 Android | [Android 开发](development/34-android-development.md) → [Android UI 设计](design/06-android-ui-design.md) |
| 调用当前 API | [API 文档](reference/09-api-documentation.md) → [OpenAPI](openapi.yaml) |
| 安装和使用 App | [用户手册](manuals/20-user-guide.md) |
| 发布 GitHub Release | [APK 更新与发布](operations/01-github-release-update.md) |
| 排查问题 | [故障排查](operations/14-troubleshooting.md) → [Bug](bug/bugList.md) → [AQ](aq/aq.md) |

## 设计与架构 · `design/`

- [01 产品设计](design/01-product-design.md)
- [02 系统架构](design/02-system-architecture.md)
- [03 数据库设计](design/03-database-design.md)
- [04 Wallet 与 Ledger](design/04-wallet-ledger-design.md)
- [05 虚拟投资](design/05-virtual-investment-design.md)
- [06 Android UI](design/06-android-ui-design.md)
- [07 实施计划](design/07-implementation-plan.md)
- [08 V1 验收标准](design/08-acceptance-criteria.md)
- [35 基础体验视觉与信息架构](design/35-family-growth-ui-breadth-baseline.md)
- [36 儿童端发展与行为设计基线](design/36-child-experience-behavioral-baseline.md)
- [37 儿童舒适品牌图标基线](design/37-child-comfort-brand-icon.md)
- [38 儿童奖励浏览与教学视频交互](design/38-child-reward-video-interaction.md)
- [39 四学段教学体验与内容分层](design/39-age-stage-teaching-experience.md)
- [40 幼儿园原创亲子活动包](design/40-kindergarten-original-parent-child-pack.md)

## 接口参考 · `reference/`

- [参考索引](reference/README.md)
- [09 API 文档](reference/09-api-documentation.md)
- [OpenAPI 3.1](openapi.yaml)

## 开发 · `development/`

- [11 开发指南](development/11-development-guide.md)
- [12 测试计划](development/12-test-plan.md)
- [17 本地开发](development/17-local-development.md)
- [26 仓库目录结构](development/26-repository-layout.md)
- [33 Spec 驱动开发](development/33-spec-driven-development.md)
- [34 Android 开发](development/34-android-development.md)

## 部署与运维 · `operations/`

- [GitHub Release APK 更新](operations/01-github-release-update.md)
- [10 部署指南](operations/10-deployment-guide.md)
- [13 运维指南](operations/13-operations-guide.md)
- [14 故障排查](operations/14-troubleshooting.md)
- [19 密码与密钥](operations/19-password-and-secrets.md)
- [22 本地部署](operations/22-local-deployment.md)
- [23 儿童数据保留、导出与恢复边界](operations/23-data-retention-and-recovery.md)
- [24 家庭成员配对与可信 TLS](operations/24-family-pairing-and-trusted-tls.md)

## 使用手册 · `manuals/`

- [手册索引](manuals/README.md)
- [20 用户手册](manuals/20-user-guide.md)
- [28 文档阅读指引](manuals/28-documentation-reading-guide.md)
- [V1 端到端场景](manuals/scenarios/family-growth-v1-e2e.md)
- [功能覆盖矩阵](manuals/scenarios/family-growth-feature-matrix.md)

## 验收 · `acceptance/`

- [15 验收报告](acceptance/15-acceptance-report.md)
- [16 已知限制](acceptance/16-known-limitations.md)

## Stage · `stages/`

- [Stage 索引](stages/README.md)
- [Stage 路线图](stages/stage-roadmap.md)
- [Stage 执行标准](stages/stage-execution-standard.md)
- [Stage 1：立项与设计](stages/stage-1-report.md)
- [Stage 2：工程骨架](stages/stage-2-report.md)
- [Stage 3：生产认证、任务审核与奖励](stages/stage-3-report.md)
- [Stage 4：Wallet 与 Ledger](stages/stage-4-report.md)
- [Stage 5：GiftMoney 与兑换](stages/stage-5-report.md)
- [Stage 6：商店、储蓄与愿望](stages/stage-6-report.md)
- [Stage 7：纯模拟基金](stages/stage-7-report.md)
- [Stage 8：Android 生产双端核心页面](stages/stage-8-report.md)
- [Stage 9：跨域闭环与家庭报告](stages/stage-9-report.md)
- [Stage 10：V1 发布、部署与总验收](stages/stage-10-report.md)
- [Stage 11：GitHub Release APK 更新](stages/stage-11-report.md)
- [Stage 12：治理对齐](stages/stage-12-report.md)
- [Stage 13：基础体验与 Android 前端](stages/stage-13-report.md)
- [Stage 14：公开 GitHub 仓库与 Release 更新链](stages/stage-14-report.md)
- [Stage 15：3 岁起儿童端发展适龄改造](stages/stage-15-report.md)
- [Stage 16：儿童舒适品牌图标与 v0.3.1 交付](stages/stage-16-report.md)
- [Stage 17：服务端零钱回收与冻结式兑现闭环](stages/stage-17-report.md)
- [Stage 18：双视角排版、奖励浏览与教学视频任务](stages/stage-18-report.md)
- [Stage 19：GitHub Release 下载 0% 真机修复](stages/stage-19-report.md)
- [Stage 20：学段底座、家长配置与分层体验路由](stages/stage-20-report.md)
- [Stage 21：共用课程、活动与学习证据引擎](stages/stage-21-report.md)
- [Stage 22：幼儿园故事舞台与亲子现实活动](stages/stage-22-report.md)
- [Stage 23：小学探索手册](stages/stage-23-report.md)
- [Stage 24：初中学科实验台](stages/stage-24-report.md)

## 项目计划与 Phase · `program/` `phases/`

- [项目总纲](program/master-plan.md)
- [项目路线图](program/roadmap.md)
- [Phase → Stage 排期](program/phase-stage-plan.md)
- [Phase 索引](phases/README.md)

## 治理、需求、缺陷与调研

- [治理入口](governance/README.md) · [行为日志](governance/activity-log.md)
- [需求规则](requirements/README.md) · [需求账本](requirements/requirement-ledger.md) · [教学与项目剩余清单](requirements/teaching-and-project-backlog.md) · [四学段体验基线](design/39-age-stage-teaching-experience.md)
- [Bug](bug/bugList.md) · [AQ](aq/aq.md) · [BLOCKERS](BLOCKERS.md)
- [调研索引](research/README.md)

## 证据 · `evidence/`

真实运行证据按 `stage-N/` 保存，不存示例成功数据：

- [Stage 1](evidence/stage-1/acceptance.json)
- [Stage 2](evidence/stage-2/acceptance.json)
- [Stage 3](evidence/stage-3/acceptance.json)
- [Stage 4](evidence/stage-4/acceptance.json)
- [Stage 5](evidence/stage-5/acceptance.json)
- [Stage 6](evidence/stage-6/acceptance.json)
- [Stage 7](evidence/stage-7/acceptance.json)
- [Stage 8](evidence/stage-8/acceptance.json)
- [Stage 9](evidence/stage-9/acceptance.json)
- [Stage 10](evidence/stage-10/acceptance.json)
- [Stage 11](evidence/stage-11/acceptance.json)
- [Stage 12](evidence/stage-12/acceptance.json)
- [Stage 13](evidence/stage-13/acceptance.json)
- [Stage 14](evidence/stage-14/acceptance.json)
- [Stage 15](evidence/stage-15/acceptance.json)
- [Stage 16](evidence/stage-16/acceptance.json)
- [Stage 17](evidence/stage-17/acceptance.json)
- [Stage 18](evidence/stage-18/acceptance.json)
- [Stage 19](evidence/stage-19/acceptance.json)
- [Stage 20](evidence/stage-20/acceptance.json)
- [Stage 21](evidence/stage-21/acceptance.json)
- [Stage 22](evidence/stage-22/acceptance.json)
- [Stage 23](evidence/stage-23/acceptance.json)
- [Stage 24](evidence/stage-24/acceptance.json)
- [Stage 25](evidence/stage-25/acceptance.json)
- [Stage 26](evidence/stage-26/acceptance.json)

## 目录约定

- 新文档必须进入对应分类并同步本索引；不得在 `docs/` 根随意新增第二事实源。
- 文档使用 UTF-8 中文；编号用于阅读顺序，同一用途不复用冲突编号。
- Stage 报告是状态真相；TODO 是执行账本；BLOCKERS 是解除条件汇总；需求账本长期保留用户原意。
- 移动文档必须更新所有链接；每次交付运行本地链接、JSON、OpenAPI 和空白检查。
