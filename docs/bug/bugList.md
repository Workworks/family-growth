# Bug 清单

发现缺陷时记录：编号、状态、现象、复现、根因、修复、回归测试与证据。

| ID | 状态 | 现象/风险 | 根因 | 修复与回归证据 |
| --- | --- | --- | --- | --- |
| BUG-001 | `CLOSED` | 孩子点击“申请零钱回收”后立即扣除 Money，绕过家长调账确认，且文案与行为不一致 | 首版基础引擎把申请和批准合并成一次账本操作 | 拆为 `PENDING → APPROVED`；申请时不改余额，家长确认后写含手续费的流水；`LocalFamilyEngineTest.withdrawalWaitsForParentApprovalThenRecordsTransparentFee` 通过 |
| BUG-002 | `FIXED_PENDING_DEVICE` | 用户在应用内下载更新时提示“APK 下载或校验失败”，无法进入系统安装确认 | 真实 v0.3.0 Release 的大小/digest/CDN 重定向当前有效；已确认客户端对 GitHub/CDN 瞬时 IO 不重试、读取窗口仅 30 秒、底层原因被通用错误覆盖，且 IO 线程直接写 Compose 状态。用户设备当时的具体 IOException 因旧版无分类日志无法反向还原 | Stage 16 已改为 30 秒连接/120 秒读取、最多 3 次 IO 重试、`identity` 传输、按百分比节流的主线程进度和超时/DNS/TLS/存储分类；来源/大小/SHA-256 仍 fail-closed。19 项 release JVM、lint、签名构建通过；待 v0.3.1 真机下载确认后关闭 |
