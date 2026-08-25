# Stage 12：参考仓库文档治理与 Agent 启动协议对齐

状态：`COMPLETED`

日期：2026-08-25

产品 Phase：计划外治理能力；全局 Stage 12

需求：REQ-014

## 目标与非目标

目标：Family Growth 的根目录 Agent 入口、`docs/` 分类、阻塞/TODO/Stage 状态职责、文档索引、开发与验收入口严格遵循 `C:\Users\70649\Desktop\aiWork\WorkThree\capital-agent-system` 的当前治理标准；以后每次对话都从仓库事实源启动，并将用户长期要求固化到 `AGENTS.md` 或 `docs/codex-skills.md`。

非目标：不复制 Capital Agent 特有的 Provider、Sidecar、Tauri 或多 Agent 产品规则；不以文档迁移改变 Stage 2、Stage 11 的运行态结论；不删除 Family Growth 已登记的需求、设计、证据或行为历史。

## 边界与不变量

- `docs/` 根保留参考标准规定的五个治理入口：`README.md`、`BLOCKERS.md`、`codex-skills.md`、`stage-current.md`、`TODO.md`；另保留参考仓库同路径的机器契约 `openapi.yaml`。
- 其余文档按 `acceptance/`、`aq/`、`bug/`、`design/`、`development/`、`evidence/`、`governance/`、`manuals/`、`operations/`、`phases/`、`program/`、`reference/`、`requirements/`、`research/`、`stages/` 分类。
- 参考仓库没有根目录 `BLOCK.md`；本项目采用其真实规范路径 `docs/BLOCKERS.md`，不创建第二份冲突真相源。
- `BLOCKERS.md` 区分外部阻塞和工程未完成；`TODO.md` 只保留未完成且可继续执行的事项；Stage 状态以对应报告为准。
- 所有现有业务文档和历史要求保留；迁移新增兼容入口或索引，不静默删除。
- 本 Stage 是纯文档治理 Stage，不修改应用代码、数据库、API、APK 或运行状态。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP12-1 | 完成 | 审计参考根入口、docs 分类和长期规则 |
| WP12-2 | 完成 | 补齐 BLOCKERS、TODO、文档索引、分类入口与仓库布局说明 |
| WP12-3 | 完成 | 重写 AGENTS 六步启动协议和 Family Growth 长期红线 |
| WP12-4 | 完成 | 结构对比、Markdown 链接、JSON、OpenAPI、空白与证据门禁 |

## 数据、API、Android 与部署变化

无运行时变化；只调整治理与文档结构。新增的 OpenAPI 文件只描述当前已实现且经代码核对的基础接口，不提前声明未来业务能力。

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V12-01 | PowerShell | 对比参考与当前 docs 一级分类、根入口 | 当前具备参考分类；根为五份治理入口和 OpenAPI 机器契约 | Stage 12 证据 |
| V12-02 | PowerShell | 解析全部 Markdown 本地链接 | 无断链 | Stage 12 证据 |
| V12-03 | PowerShell | 解析 evidence JSON 与 `openapi.yaml` | JSON/YAML 可解析 | Stage 12 证据 |
| V12-04 | Git | `git diff --check` | 无空白错误 | Stage 12 证据 |
| V12-05 | 人工核对 | 检查 AGENTS 每次任务六步协议和长期要求 | 与参考标准职责一致，并保留本项目红线 | Stage 12 报告 |

## 完成标准

- [x] AC12-01 `PASS`：`docs/` 根六个真实文件与参考仓库一致；参考的全部一级分类均存在，额外仅保留 P0 要求的 `requirements/`。
- [x] AC12-02 `PASS`：BLOCKERS 区分外部/工程缺口，TODO 仅保留可执行项，current 与 Stage 报告同步。
- [x] AC12-03 `PASS`：AGENTS 固化每次对话 Step 1–6、需求永久登记、固定汇报格式及十二条项目红线。
- [x] AC12-04 `PASS`：开发、测试、布局、手册、验收、Phase、Reference、Research、Operations 和 Governance 均有索引入口。
- [x] AC12-05 `PASS`：REQ-014、行为日志、路线图、Stage 索引和 evidence 已完成回链。
- [x] AC12-06 `PASS`：62 份 Markdown 的断链/可达性/Stage 状态检查、全部 evidence JSON、OpenAPI YAML 和 `git diff --check` 通过。

## 安全检查、限制与交接

不读取或写入 secret，不复制参考项目的 `.env`。参考项目的目录和治理职责是模板，Family Growth 的儿童隐私、账本、模拟投资与 Android 安全红线继续优先适配。后续每个 Agent 会话必须按根 `AGENTS.md` 从当前事实源重新加载。

## 实施结果与交接

- 参考仓库实际使用 `docs/BLOCKERS.md`，没有根 `BLOCK.md`；本项目只保留同一路径，避免双事实源。
- 新增参考同类的 `manuals/`、`phases/`、`research/` 入口和 development/operations/acceptance/reference 文档集。
- 新增当前六个基础创建接口的 `openapi.yaml`，没有提前声明认证、钱包或基金接口。
- 新增 `scripts/Test-DocumentationLinks.ps1`，检查相对链接、总索引可达性、合法 Stage 状态及报告/roadmap/index 一致性。
- 结构化证据：[Stage 12 acceptance](../evidence/stage-12/acceptance.json)。本 Stage 不修改运行时代码，不需要重跑 Maven/Android 门禁。

后续交接：每次对话严格执行 AGENTS Step 1–6；新增长期要求先更新 AGENTS/codex-skills 和需求账本，再实施。
