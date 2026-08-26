# Bug 清单

发现缺陷时记录：编号、状态、现象、复现、根因、修复、回归测试与证据。

| ID | 状态 | 现象/风险 | 根因 | 修复与回归证据 |
| --- | --- | --- | --- | --- |
| BUG-001 | `CLOSED` | 孩子点击“申请零钱回收”后立即扣除 Money，绕过家长调账确认，且文案与行为不一致 | 首版基础引擎把申请和批准合并成一次账本操作 | 拆为 `PENDING → APPROVED`；申请时不改余额，家长确认后写含手续费的流水；`LocalFamilyEngineTest.withdrawalWaitsForParentApprovalThenRecordsTransparentFee` 通过 |
| BUG-002 | `CLOSED_SUPERSEDED` | 用户在应用内下载更新时提示“APK 下载或校验失败”，无法进入系统安装确认 | 真实 v0.3.0 Release 的大小/digest/CDN 重定向当前有效；已确认客户端对 GitHub/CDN 瞬时 IO 不重试、读取窗口仅 30 秒、底层原因被通用错误覆盖，且 IO 线程直接写 Compose 状态。用户设备当时的具体 IOException 因旧版无分类日志无法反向还原 | Stage 16 的有限重试和错误分类已交付；首字节 0% 后续由 BUG-004/Stage 19 接管并取得 v0.3.3→v0.3.4 真机成功反馈，来源/大小/SHA-256 全程 fail-closed |
| BUG-003 | `FIXED_PENDING_DEVICE` | 家长/孩子视角切换按钮在现有顶栏和侧栏中位置分散、标签不完整，平板与手机上的层级不清楚 | 角色切换复用了紧凑 `AssistChip`，同时在平板导航区重复出现，未形成稳定的二选一控件 | Stage 18 已改为统一双段式“孩子视角 / 家长视角”；手机置于标题下、平板置于固定侧栏，最小高度 48dp，家长 PIN 保持。debug/release 单测、lint/build 通过；真实平板布局待用户测试 |
| BUG-004 | `CLOSED` | v0.3.1 能发现 v0.3.2，但点击下载后长期停在“下载并校验 0%” | 已确认旧客户端只在收到首批字节后更新 UI，首次读取允许阻塞 120 秒；GitHub Release CDN 无首字节或网络切换时既无阶段提示，也没有系统下载调度/备用入口 | v0.3.3 改为系统 DownloadManager、真实阶段、45 秒卡顿切换官方 Asset API、取消/发布页退路和下载后 SHA-256；用户确认 v0.3.3→v0.3.4 可正常更新，2026-08-26 关闭 |
