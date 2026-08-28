# Family Growth 功能覆盖矩阵

| 功能域 | 当前状态 | 目标 Stage | 真实证据 |
| --- | --- | ---: | --- |
| Family/Parent/Child/Plan/Goal/Task 基础 | 自动化已实现 | 2 | Stage 2 evidence |
| Android 双模式与响应式前端 | 双段视角和家长五区/儿童三区离线构建通过，运行阻塞 | 2、8、13、18 | Stage 13/18 evidence/BLOCKERS |
| 任务提交、审核与奖励 | 服务端生产闭环与 Android 本地基础均通过自动化 | 3、13 | Stage 3/13 evidence |
| Wallet/Ledger | 服务端调账、幂等、并发与对账闭环通过 | 4、13 | Stage 4 evidence |
| 压岁钱、兑换与零钱回收待审 | 礼金/双向兑换服务端通过；零钱回收为本机透明待审基础，服务端冻结/PAID 尚缺 | 5、10、13 | Stage 5/10/13 evidence |
| 奖励商店、储蓄与愿望 | 服务端 H2/PostgreSQL 闭环通过 | 6、13 | Stage 6 evidence |
| 儿童奖励浏览与教学视频任务 | Android 离线实现、JVM/lint/build/签名通过；真机交互待验收 | 18 | Stage 18 报告/evidence |
| 纯模拟基金 | 服务端 NAV/费用/持仓/P&L 与 3–5 岁门禁通过 | 7、13 | Stage 7 evidence |
| PIN、防沉迷、今日使用与报告 | 生产认证/分钟事件/事实报告通过；禁用时段与临时放行审计尚缺 | 8–10、13 | Stage 8–10 evidence/BLOCKERS |
| 0.3.2 稳定 APK | 本地/CI 同签名、版本、包名、公开 digest 和下载复验通过；安装阻塞 | 10、11、14–16、18 | Stage 18 evidence/BLOCKERS |
| Stage 3–10 生产服务端深化 | Stage 3–7/9 自动化完成；Stage 8/10 真机阻塞且深度缺口显式保留 | 3–10 | Stage 3–10 evidence / Stage 路线图 |
| V1 总验收 | 自动化/恢复/远端 Release 通过；真机未完成 | 10 | Stage 10 evidence |
| GitHub Release 更新实现 | v0.2.0–v0.3.5 九个同签名正式版本已发布；v0.3.3 起使用系统下载与官方 Asset API 回退，v0.3.3→v0.3.4 已获真机成功反馈，最新 v0.3.4→v0.3.5 待验收 | 11、14–16、18–21 | Stage 19/21 evidence、BLOCKERS |
