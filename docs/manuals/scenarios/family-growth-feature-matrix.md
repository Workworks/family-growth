# Family Growth 功能覆盖矩阵

| 功能域 | 当前状态 | 目标 Stage | 真实证据 |
| --- | --- | ---: | --- |
| Family/Parent/Child/Plan/Goal/Task 基础 | 自动化已实现 | 2 | Stage 2 evidence |
| Android 双模式与五区响应式前端 | 本地基础实现/构建通过，运行阻塞 | 2、8、13 | Stage 13 evidence/BLOCKERS |
| 任务提交、审核与奖励 | 服务端生产闭环与 Android 本地基础均通过自动化 | 3、13 | Stage 3/13 evidence |
| Wallet/Ledger | 服务端调账、幂等、并发与对账闭环通过 | 4、13 | Stage 4 evidence |
| 压岁钱、兑换与零钱回收待审 | 礼金/双向兑换服务端通过；零钱回收为本机透明待审基础，服务端冻结/PAID 尚缺 | 5、10、13 | Stage 5/10/13 evidence |
| 奖励商店、储蓄与愿望 | 服务端 H2/PostgreSQL 闭环通过 | 6、13 | Stage 6 evidence |
| 纯模拟基金 | 服务端 NAV/费用/持仓/P&L 与 3–5 岁门禁通过 | 7、13 | Stage 7 evidence |
| PIN、防沉迷、今日使用与报告 | 生产认证/分钟事件/事实报告通过；禁用时段与临时放行审计尚缺 | 8–10、13 | Stage 8–10 evidence/BLOCKERS |
| 0.3.0 稳定 APK | 本地同签名、版本、包名和 SHA-256 通过；远端 tag/Release 待执行，安装阻塞 | 10、11、14、15 | Stage 10 evidence/BLOCKERS |
| Stage 3–10 生产服务端深化 | Stage 3–7/9 自动化完成；Stage 8/10 真机阻塞且深度缺口显式保留 | 3–10 | Stage 3–10 evidence / Stage 路线图 |
| V1 总验收 | 自动化/恢复通过；远端 Release 与真机未完成 | 10 | Stage 10 evidence |
| GitHub Release 更新实现 | 公开三版已发布、真机覆盖升级阻塞 | 11、14、15 | Stage 15 evidence/BLOCKERS |
