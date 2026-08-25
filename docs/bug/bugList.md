# Bug 清单

发现缺陷时记录：编号、状态、现象、复现、根因、修复、回归测试与证据。

| ID | 状态 | 现象/风险 | 根因 | 修复与回归证据 |
| --- | --- | --- | --- | --- |
| BUG-001 | `CLOSED` | 孩子点击“申请零钱回收”后立即扣除 Money，绕过家长调账确认，且文案与行为不一致 | 首版基础引擎把申请和批准合并成一次账本操作 | 拆为 `PENDING → APPROVED`；申请时不改余额，家长确认后写含手续费的流水；`LocalFamilyEngineTest.withdrawalWaitsForParentApprovalThenRecordsTransparentFee` 通过 |
