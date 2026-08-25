# Stage 执行与维护标准

## 1. 状态

仅使用 `NOT_STARTED`、`IN_PROGRESS`、`COMPLETED`、`BLOCKED`。`COMPLETED` 必须有代码/文档产物、可复现命令、退出码和证据；不能以接口空壳、Mock 页面、计划文档或单纯构建成功代替真实完成。

## 2. 每阶段必备内容

每份 Stage 报告必须维护：

1. 全局 Stage、产品 Phase、关联需求；
2. 目标与非目标；
3. 前置输入、范围内/外、外部前置和安全不变量；
4. 编号工作包及状态；
5. 数据库、API、Android、部署和文档变化；
6. 验证矩阵，区分静态、自动化、目标数据库、安装态、真机和 E2E；
7. 编号完成标准及 `PASS / FAIL / BLOCKED` 结果；
8. 安全检查、证据目录、已知限制和下一阶段交接。

进入 `IN_PROGRESS` 前必须写清目标、边界、验证方式和编号完成标准；缺任一项保持 `NOT_STARTED`。实施与验收逐条引用工作包和完成标准，未取得证据的条目不得勾选。

`stage-current.md` 是当前上下文快照，`TODO.md` 是未完成执行账本，`BLOCKERS.md` 集中说明解除条件；状态事实始终以 Stage 报告为准。状态或优先级变化时同步三者、roadmap、Stage 索引和 docs 索引。

纯文档 Stage 允许明确“无代码、数据库、API、Android 和部署变化”，完成定义改为：事实源一致、需求可追踪、结构与链接可验证、机器契约可解析、没有误改运行时状态。纯文档 Stage 不得宣称未来业务能力完成。

## 3. 通用完成定义

1. 实现遵守模块依赖、账本、儿童隐私和 Android 安全边界。
2. 新表只由 Flyway 创建，JPA 使用 `ddl-auto=validate`。
3. API 有正向、401/403/404 反向、校验、冲突和幂等测试。
4. Android 通过单测、Compose UI、lint 和构建；安装能力必须另有目标设备证据。
5. 敏感信息不进入日志、Git、APK、Release 和测试证据。
6. 命令、结果、限制、需求和状态回填到报告及证据。
7. `git diff --check`、Markdown 链接、证据 JSON 和 OpenAPI 解析通过。

## 4. 证据命名

证据存 `docs/evidence/stage-N/`。JSON 至少包含 `capturedAt`、`command`、`workingDirectory`、`shellType`、`exitCode`、`passed` 和摘要；关键产物包含 SHA-256 与大小。大日志不提交，只记录位置、哈希和脱敏摘要。

## 5. Stage 收口

完成后从 TODO 删除对应执行项；若仍缺外部条件则状态保持 `BLOCKED` 并进入 BLOCKERS。门禁通过后按 Stage 独立 Conventional Commit，记录真实 SHA；初始未提交仓库或用户未授权提交时，明确说明“未提交”，不得伪造。
