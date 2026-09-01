# API 文档

基础路径：`/api/v1`。Stage 3 起采用服务端生产认证：除 bootstrap/login 外，所有业务接口必须携带 `Authorization: Bearer <opaque-token>`；数据库只保存 token SHA-256 哈希。家长 PIN 为 6 位且使用 BCrypt cost 12 保存，连续 5 次失败锁定 15 分钟。

## 认证与角色

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/bootstrap` | 公开 | 原子创建新家庭、首位家长、PIN 凭据和 12 小时家长会话 |
| POST | `/auth/login` | 公开 | 家长 PIN 登录；失败统一 401，锁定返回 429 |
| POST | `/auth/child-sessions` | PARENT | 为本家庭孩子创建 12 小时受限会话 |

不接受客户端自报 `role/familyId/childId` 作为授权事实。PARENT/CHILD、家庭和孩子范围来自服务端会话；跨家庭或无权对象与不存在对象统一返回 404。

## 家庭成长与任务奖励

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/families/{familyId}/children` | PARENT | 创建孩子并初始化 XP/Wallet |
| POST | `/families/{familyId}/children/{childId}/plans` | PARENT | 创建成长计划 |
| POST | `/families/{familyId}/plans/{planId}/goals` | PARENT | 创建成长目标 |
| POST | `/families/{familyId}/goals/{goalId}/tasks` | PARENT | 创建学习/成长任务 |
| POST | `/families/{familyId}/children/{childId}/tasks/{taskId}/completions` | CHILD 本人 | 提交 TaskCompletion，必须有 `Idempotency-Key` |
| POST | `/families/{familyId}/completions/{completionId}/review` | PARENT | 批准/拒绝并原子发放 XP/Coin/Money，必须有 `Idempotency-Key` |
| GET | `/families/{familyId}/children/{childId}/wallet` | PARENT/CHILD 本人 | 查询 Money 总额、冻结额、可用额和 Coin 当前余额 |
| GET | `/families/{familyId}/children/{childId}/ledger?limit=50` | PARENT/CHILD 本人 | 查询最近不可变 Money/Coin 流水 |
| POST | `/families/{familyId}/children/{childId}/wallet/adjustments` | PARENT | 带原因和幂等键执行 Money/Coin 正负调账，禁止负余额 |
| GET | `/families/{familyId}/children/{childId}/wallet/reconciliation` | PARENT | 汇总 Ledger delta 并与 Wallet 对账 |
| POST | `/families/{familyId}/children/{childId}/gift-money` | PARENT | 幂等登记压岁钱并原子写入 Money 流水 |
| POST | `/families/{familyId}/exchange-rules` | PARENT | 创建新的双向比例、费用与单笔预算规则版本 |
| GET | `/families/{familyId}/exchange-rules/active` | PARENT/CHILD | 查询家庭当前适用规则 |
| POST | `/families/{familyId}/children/{childId}/exchange-previews` | PARENT/CHILD 本人 | 保存十分钟有效的本金、比例、费用、净额和教育声明快照 |
| POST | `/families/{familyId}/exchange-previews/{previewId}/confirm` | PARENT/CHILD 本人 | 以幂等键确认；规则未漂移时原子写 Money/Coin 双分录 |
| GET/POST | `/families/{familyId}/reward-products` | PARENT/CHILD / PARENT | 查询可用家庭奖励；家长配置 Coin 成本、库存和启用状态 |
| POST | `/families/{familyId}/children/{childId}/reward-orders` | PARENT/CHILD 本人 | 幂等创建奖励订单，不预扣 Coin |
| POST | `/families/{familyId}/reward-orders/{orderId}/review` | PARENT | 批准时才原子扣 Coin/库存；拒绝不扣款 |
| POST | `/families/{familyId}/reward-orders/{orderId}/cancel` | PARENT/CHILD 本人 | 取消 CREATED 订单，不扣款 |
| GET/POST | `/families/{familyId}/children/{childId}/saving[/transfers]` | PARENT/CHILD 本人 | 查询内部储蓄，或幂等转入/转出并保持总 Money 守恒 |
| GET/POST | `/families/{familyId}/children/{childId}/wishes` | PARENT/CHILD 本人 | 查询/创建愿望目标 |
| POST | `/families/{familyId}/wishes/{wishId}/allocation` | PARENT/CHILD 本人 | 显式分配真实储蓄，所有愿望总分配不得超过 Saving |
| GET/POST | `/families/{familyId}/funds` | PARENT/CHILD / PARENT | 查询/创建家庭纯模拟基金，固定返回风险教育声明 |
| POST | `/families/{familyId}/funds/{fundId}/nav` | PARENT | 写入可涨可跌的日期 NAV，限制单期 ±50% 且日期唯一 |
| POST | `/families/{familyId}/funds/{fundId}/fee-rules` | PARENT | 创建版本化买入/卖出费率 |
| POST | `/families/{familyId}/children/{childId}/funds/{fundId}/trade-previews` | PARENT/适龄 CHILD 本人 | 预览 NAV、费用、净额和份额；3–5 岁 CHILD 会话拒绝 |
| POST | `/families/{familyId}/fund-trade-previews/{previewId}/confirm` | PARENT/适龄 CHILD 本人 | 幂等确认，NAV/规则漂移返回 409 |
| GET | `/families/{familyId}/children/{childId}/funds/{fundId}/position` | PARENT/CHILD 本人 | 返回份额、加权成本、市值和已实现/未实现损益 |
| POST | `/families/{familyId}/withdrawal-rules` | PARENT | 幂等创建版本化线下兑现比例、比例费与固定费；默认 1:1、零费用 |
| GET | `/families/{familyId}/withdrawal-rules/active` | PARENT/CHILD | 查询家庭当前零钱回收规则 |
| POST | `/families/{familyId}/children/{childId}/withdrawal-quotes` | PARENT/CHILD 本人 | 幂等生成十分钟有效的 Money、比例、gross、fee、net 与线下声明快照 |
| GET/POST | `/families/{familyId}/children/{childId}/withdrawal-requests` | PARENT/CHILD 本人 | 查询或基于未过期报价幂等提交申请；提交不扣账、不冻结 |
| POST | `/families/{familyId}/withdrawal-requests/{requestId}/approve` | PARENT | 幂等批准并冻结 Money，其他支出不能穿透冻结额 |
| POST | `/families/{familyId}/withdrawal-requests/{requestId}/reject` | PARENT | 幂等拒绝，不扣账 |
| POST | `/families/{familyId}/withdrawal-requests/{requestId}/cancel` | REQUESTED 本人/PARENT；APPROVED 仅 PARENT | 取消待审或释放已批准冻结额 |
| POST | `/families/{familyId}/withdrawal-requests/{requestId}/paid` | PARENT | 确认已线下支付；原子减少总额/冻结额并追加 WITHDRAWAL Money 流水 |
| GET | `/families/{familyId}/children/{childId}/sync` | PARENT/CHILD 本人 | Android 聚合同步任务/最新 Completion、钱包和今日审核摘要 |
| GET/PUT | `/families/{familyId}/children/{childId}/usage-policy` | PARENT/CHILD 本人 / PARENT | 查询或配置家庭时区、每日/单次 App 内时长上限和睡眠时段；缺省为 Asia/Shanghai、20/10 分钟、21:30–06:30 |
| GET | `/families/{familyId}/children/{childId}/usage-access` | PARENT/CHILD 本人 | 返回服务端对睡眠时段、日上限和有效临时放行的统一判定 |
| POST | `/families/{familyId}/children/{childId}/usage-allowances` | PARENT | 幂等创建 1–60 分钟临时放行，必须说明原因并自动到期 |
| POST | `/families/{familyId}/children/{childId}/usage-events` | PARENT/CHILD 本人 | 幂等记录本 App 的活跃/学习分钟；只接受最近 31 天且不超过未来 5 分钟的事件 |
| GET | `/families/{familyId}/children/{childId}/reports/today` | PARENT/CHILD 本人 | 按家庭时区返回本人适龄使用、任务、待审核和钱包摘要 |
| GET | `/families/{familyId}/children/{childId}/reports/monthly` | PARENT | 从 Usage/Completion/Ledger/Saving/Fund 事实表聚合月度成长和财商报告 |
| GET/PUT | `/families/{familyId}/children/{childId}/experience-profile` | PARENT/CHILD 本人 / PARENT | 查询服务端推荐/有效学段、小学低/高年级带与反馈档案；家长按版本修改出生日期、覆盖学段、小学分段和触觉开关。`primaryBandOverride` 仅在有效学段为 `PRIMARY` 时允许 |
| GET | `/families/{familyId}/children/{childId}/experience-profile/audit` | PARENT | 查询出生日期、覆盖学段、小学分段和触觉配置的不可变审计记录 |
| GET | `/families/{familyId}/children/{childId}/experience-profile/transition-preview` | PARENT | 修改年龄/覆盖学段前预览旧学段未开始自主课程归档和新学段恢复数量；历史证据不删除 |
| POST | `/families/{familyId}/children/{childId}/data-rights/exports` | PARENT | 幂等生成稳定 schema 的儿童数据 JSON；排除 PIN/Token/secret、答案键和家长私密说明 |
| POST | `/families/{familyId}/children/{childId}/data-rights/erasure-preview` | PARENT | 返回明确删除/保留清单和十分钟一次性确认 token |
| POST | `/families/{familyId}/children/{childId}/data-rights/erasures/{requestId}/confirm` | PARENT | 用一次性 token 与服务端 6 位 PIN 确认去标识化；保留账本、必要审计和幂等事实 |
| GET/POST | `/families/{familyId}/documentary-sources` | PARENT | 查询或幂等创建带权利依据、访问模式、学段和生命周期的纪录片来源 |
| GET | `/families/{familyId}/children/{childId}/documentaries` | PARENT/CHILD 本人 | 只投影有效学段内已批准且未过期的条目；孩子响应不含可启动 URL 或权利元数据 |
| POST | `/families/{familyId}/documentary-sources/{sourceId}/approve` | PARENT | 幂等批准 DRAFT 来源 |
| POST | `/families/{familyId}/documentary-sources/{sourceId}/withdraw` | PARENT | 幂等撤回来源并保留历史 |
| GET/POST | `/families/{familyId}/education-resource-sources` | PARENT | 查询或幂等创建免费教育来源；只接受公共、无凭据/查询/片段、默认 443 的 HTTPS 主机名 |
| POST | `/families/{familyId}/education-resource-sources/{sourceId}/refresh` | PARENT | 受限读取 HTML 导航栏目；逐跳复验 DNS/同源重定向，失败保留最近成功快照 |
| POST | `/families/{familyId}/education-resource-sources/{sourceId}/approve` | PARENT | 成功读取非空栏目后幂等批准，允许进入适龄儿童投影 |
| POST | `/families/{familyId}/education-resource-sources/{sourceId}/withdraw` | PARENT | 幂等撤回并保留来源、栏目和动作历史 |
| GET | `/families/{familyId}/children/{childId}/education-resource-catalog` | PARENT/CHILD 本人 | 只返回匹配有效学段的已批准来源名/栏目名/同步时间，不返回任何 URL 或使用说明；幼儿园为空 |
| GET/POST | `/families/{familyId}/teaching/courses` | PARENT | 查询课程版本或原子创建 Course 与首个嵌套 DRAFT；活动答案键仅写入服务端 |
| POST | `/families/{familyId}/teaching/courses/{courseId}/versions` | PARENT | 创建新 DRAFT 版本；不原地修改已发布内容 |
| GET | `/families/{familyId}/teaching/course-versions/{versionId}` | PARENT | 读取一个不可变课程版本的课节树，供家长端选择并布置；CHILD 禁止读取 |
| POST | `/families/{familyId}/teaching/course-versions/{versionId}/publish` | PARENT | 幂等发布课程版本并记录发布人/时间，发布后没有内容修改 API |
| GET/POST | `/families/{familyId}/children/{childId}/learning/assignments` | PARENT/CHILD 本人 / PARENT | 查询本人适龄已发布课节，或由家长分配一个课节；孩子响应不含答案键和权利依据 |
| GET/PUT | `/families/{familyId}/children/{childId}/autonomous-learning/reward-policy` | PARENT | 查询或按期望版本更新固定 Money/Coin/XP 奖励；默认全零，修改只影响之后新加入的 Assignment，并追加审计 |
| POST | `/families/{familyId}/children/{childId}/autonomous-learning/sync` | PARENT/CHILD 本人 | 显式幂等加入当前有效学段、每门课程最新已发布版本的缺失课节；GET 目录保持只读，创建时固化奖励快照 |
| GET | `/families/{familyId}/children/{childId}/autonomous-learning/primary-report` | PARENT | 最近 7 天明确学习分钟与历史小学 Assignment/求助/再练事实；无评分、能力推断、答案键或家长私密说明，切换学段后仍保留历史 |
| GET | `/families/{familyId}/children/{childId}/junior-learning/plan` | PARENT/CHILD 本人 | 读取当前初中未开始自主课程顺序；进行中、待回应、已完成和返工项不进入可排序投影 |
| POST | `/families/{familyId}/children/{childId}/junior-learning/plan/move` | PARENT/CHILD 本人 | 按计划 revision 幂等上移/下移未开始项；跨家庭隔离、冲突返回 409，每次有效移动追加不可变审计 |
| GET | `/families/{familyId}/children/{childId}/junior-learning/report` | PARENT | 最近 7 天明确学习分钟、各学科 Assignment/求助/再练事实与计划版本；不含评分、能力推断、答案键或私密说明 |
| GET/PUT | `/families/{familyId}/children/{childId}/senior-learning/modules` | PARENT；GET 允许 CHILD 本人 | 读取或由家长按 revision 幂等配置高中必修/选择性必修/选修边界；保留不可变审计和历史事实 |
| GET/POST | `/families/{familyId}/children/{childId}/senior-learning/goals` | PARENT/CHILD 本人读取；CHILD 本人创建 | 在已启用模块内建立本周目标、证据要求和下一行动；不形成掌握、不结算奖励 |
| PUT/POST | `/families/{familyId}/children/{childId}/senior-learning/goals/{goalId}`、`.../archive` | CHILD 本人 | 按 revision 更新或归档本人目标；幂等、跨家庭隔离，历史不删除 |
| GET/POST | `/families/{familyId}/children/{childId}/senior-learning/reflections` | PARENT/CHILD 本人读取；CHILD 本人追加 | 保存不可覆盖的证据摘要、策略、下一行动和主动求助；不含能力标签 |
| GET | `/families/{familyId}/children/{childId}/senior-learning/report` | PARENT | 最近 7 天学习分钟、Assignment 状态、目标、复盘和求助事实；不生成评分、预测、风险或能力推断 |
| POST | `/families/{familyId}/children/{childId}/learning/assignments/{assignmentId}/activities/{activityId}/attempts` | CHILD 本人 | 幂等记录活动尝试；视频须报告至少 90% 实际播放计数，客观题由服务端判定，现实活动只记 ATTEMPTED |
| POST | `/families/{familyId}/children/{childId}/learning/assignments/{assignmentId}/submit` | CHILD 本人 | required evidence 齐备且 `expectedVersion` 一致时提交课节；返工后还必须先产生新的 Attempt |
| POST | `/families/{familyId}/children/{childId}/learning/assignments/{assignmentId}/review` | PARENT | `APPROVE` 追加家长确认与 MASTERED，并在同一事务按 Assignment 快照结算一次 XP/Money/Coin；`REWORK` 不发奖 |
| GET/POST | `/families/{familyId}/children/{childId}/growth-archive/plans` | PARENT | 查询或幂等创建成长计划；创建只能从 DRAFT/ACTIVE 开始，不接受伪造终态 |
| POST | `/families/{familyId}/children/{childId}/growth-archive/plans/{planId}/transition` | PARENT | 按 revision 幂等推进 DRAFT/ACTIVE/PAUSED/COMPLETED/CANCELED 状态机，终态不可重开 |
| GET/POST | `/families/{familyId}/children/{childId}/growth-archive/plans/{planId}/goals` | PARENT | 查询或幂等创建计划内目标；终态计划不能追加目标 |
| POST | `/families/{familyId}/children/{childId}/growth-archive/goals/{goalId}/transition` | PARENT | 按 revision 将 ACTIVE 目标完成或取消，终态不可重开 |
| GET/POST/PUT | `/families/{familyId}/children/{childId}/growth-archive/milestones` | PARENT | 查询、幂等创建或按 revision 更新具体、可观察的成长记录 |
| POST/GET | `/families/{familyId}/children/{childId}/growth-archive/milestones/{milestoneId}/artifacts`、`.../artifacts/{artifactId}` | PARENT | 上传并读取 JPEG/PNG/WebP 成长照片；服务端核验魔数和 5 MiB 上限，响应 no-store；导出只含元数据 |
| GET | `/families/{familyId}/children/{childId}/growth-archive/parent-record-templates` | PARENT | 返回 0–2 岁亲子阅读、运动、语言、自理与情感连接的家长记录模板 |
| GET | `/families/{familyId}/children/{childId}/growth-archive/report` | PARENT | 只聚合计划、记录、图片和既有任务事实，不产生评分、人格标签或能力推断 |
| POST/GET | `/families/{familyId}/children/{childId}/reward-budget-rules`、`.../active` | PARENT | 配置并读取版本化日/周/月奖励 Money 预算与超限策略；旧版本和审计不覆盖 |
| GET/POST | `/families/{familyId}/children/{childId}/reward-budget-summary`、`.../reward-budget-overrides` | PARENT | 读取当前已用/剩余额度，或为精确 Completion 与奖励摘要创建一次性超限覆盖 |
| POST/GET | `/families/{familyId}/exchange-control-rules`、`.../active` | PARENT 写；PARENT/CHILD 本人读 | 配置兑换方向、日/月 source 额度及儿童是否先经家长批准；规则版本随 Preview 固化 |
| GET/POST | `/families/{familyId}/children/{childId}/exchange-approval-requests` | PARENT/CHILD 本人读；CHILD 本人提交 | 待审批只保存家庭请求且不扣账；孩子端仅展示中性回应状态 |
| POST | `/families/{familyId}/exchange-approval-requests/{requestId}/review` | PARENT | 批准时在同一事务复验额度并确认兑换，拒绝不产生账本变化 |
| POST | `/families/{familyId}/reward-orders/{orderId}/fulfill` | PARENT | 将已批准实体奖励一次性标记为 FULFILLED 并保存现实履约备注；不再次改变 Wallet/Ledger |
| POST | `/auth/parent-invitations/accept` | 公开短码入口 | 十分钟一次性邀请换取 GUARDIAN 会话并设置独立 BCrypt PIN；失败统一 401 |
| GET/POST | `/families/{familyId}/parent-invitations`、`.../{invitationId}/revoke` | OWNER | 创建只展示一次的邀请码或撤销未接受邀请；幂等重放不再次返回代码 |
| GET | `/families/{familyId}/members` | PARENT | 查看 OWNER/GUARDIAN 与 ACTIVE/REVOKED 状态 |
| POST | `/families/{familyId}/members/{parentId}/reset-pin`、`.../revoke` | OWNER | 重置另一成员 PIN 或撤销 GUARDIAN，并立即撤销其全部会话；OWNER 不能撤销自己 |
| GET | `/families/{familyId}/children` | PARENT | 返回最小孩子选择列表；切换后必须重新签发 child-scoped session |
| POST | `/families/{familyId}/device-pairings` | PARENT | 创建五分钟、单次使用的 PARENT/CHILD 设备配对码；代码只展示一次 |
| POST | `/auth/device-pairings/accept` | 公开短码入口 | 在可信 HTTPS 上换取绑定 `device_id` 的角色会话；并发消费仅一台成功 |
| GET/POST | `/families/{familyId}/devices`、`.../{deviceId}/revoke` | PARENT | 查看设备或撤销设备及其全部绑定会话 |
| GET/POST | `/families/{familyId}/notifications`、`.../{notificationId}/read` | 当前成员 | 最小应用内待办与已读；不改变 Wallet、Ledger、Completion 或源业务状态 |
| GET/PUT | `/families/{familyId}/economy-lab`、`.../saving-rule` | PARENT | 查询家庭经济实验室或创建新版本储蓄奖励规则；孩子不能修改 |
| POST | `/families/{familyId}/economy-lab/children/{childId}/saving-awards` | PARENT | 按孩子/周期幂等结算一次；奖励与存入两条 Money 流水同组且 Saving 同事务增加 |
| PUT/POST | `/families/{familyId}/economy-lab/funds/{fundId}/market-rule`、`.../ticks` | PARENT | 配置固定种子/最大日波动并生成确定性教学 NAV；同基金/日期返回同一事实，不接真实行情 |
| PUT | `/families/{familyId}/economy-lab/funds/{fundId}/holding-fee-rule` | PARENT | 配置最低持有天数与提前费率；卖出预览/订单固化持有天数、提前费、总费和净额 |
| POST | `/families/{familyId}/economy-lab/cosmetics` | PARENT | 创建固定 Coin 价格装扮，继续走奖励申请/批准/履约；无随机奖励或自动扣账 |

批准审核会锁定 Completion 与 Wallet；Coin/Money 流水先追加，再在同一事务更新余额和 Completion。重复提交返回首次结果；重复/冲突审核返回 409，不能重复发奖。XP 的奖励事实保存在 Completion 快照并更新 `child_progress`。

奖励预算在普通任务审核事务内裁决实际 Money/Coin/XP；超额转换使用规则固定比例并向下取整，提议值与实际值同时留痕。兑换额度在 Preview 阶段提供即时反馈，在 Confirm 阶段通过日/月额度桶的条件原子更新占用，后续钱包或流水失败会整体回滚，因此并发确认最多一笔穿过剩余额度。审批请求和 `FULFILLED` 履约事实本身均不制造账本变化。

自主学习奖励与普通成长任务奖励共用 Wallet/Ledger 不变量，但业务类型为 `LEARNING_ASSIGNMENT`。策略修改不追溯已有 Assignment；零奖励只记录结算时间，不生成零金额流水。自动同步只投影当前有效学段的最新已发布课程版本，历史 Attempt/完成记录继续保留。

零钱回收状态机为 `REQUESTED → APPROVED → PAID`、`REQUESTED → REJECTED/CANCELLED`、`APPROVED → CANCELLED`。`availableMoney = moneyBalance - reservedMoney`；家长调账、Money→Coin、储蓄转入和模拟基金买入都必须检查可用额。APPROVED 只冻结，PAID 才按申请 Money 扣账；手续费只影响家庭约定的线下净到账，并以不可变快照和流水原因透明保存。

月度报告不保存第二份余额：Money/Coin 收支来自不可变 Ledger，压岁钱、兑换费、储蓄和模拟基金分别来自业务事实表；`walletLedgerBalanced` 直接比较 Wallet 与全量流水。CHILD 会话不能读取月度财务报告，也不能读取其他孩子的今日摘要。

学段推荐边界为 0–2 岁 `PARENT_ONLY`、3–5 岁 `KINDERGARTEN`、6–11 岁 `PRIMARY`、12–14 岁 `JUNIOR_MIDDLE`、15 岁及以上 `SENIOR_HIGH`。家长覆盖不能选择 `PARENT_ONLY`，且必须说明原因；更新使用 `expectedVersion` 防止并发覆盖并追加审计。纪录片 `OFFICIAL_LINK` 只接受无凭据 HTTPS 地址且始终要求家长操作；原创离线和已授权离线内容分别只接受 `asset://`、`content-package://` 引用。目录仅证明来源已登记和审批，不证明第三方内容已获下载、剪辑或再分发授权。

有效学段变化采用“先预览、后更新”：只归档旧学段中尚未开始的自主 Assignment，并恢复目标学段过去被该机制归档的 Assignment。进行中、待回应、返工、完成、家长手动布置、Attempt、Completion、奖励和 Ledger 均不删除或改写。儿童数据删除是受约束去标识化，不承诺物理备份即时擦除；直接标识、儿童自由文本、愿望标题、非财务使用明细、临时放行和有效儿童会话会被删除或替换，账本、必要审计、幂等及最小学习状态保留。

家庭协作短码仅保存 SHA-256 摘要，邀请十分钟、设备码五分钟后失效且只允许消费一次。Android 正式构建仅接受系统信任的 HTTPS，不提供忽略证书错误。待办只投影任务审核、兑换审核、奖励审核/兑现和课程更新；儿童不接收系统推送、红点倒计时、声音或震动。儿童数据删除会同步去标识化相关待办文案与儿童设备名称，协作审计只保留最小安全事实。

免费教育来源发现不是通用爬虫：服务端不执行 JavaScript、不提交表单、不携带 Cookie/认证，只读取最多 512 KiB HTML，最多跟随 3 次同源重定向并保存 30 个同源导航栏目。URL 创建、DNS 结果和每次重定向均拒绝 loopback、私网、链路本地、组播、保留地址和 IP 字面量。成功刷新会把来源重新置为 `DRAFT`，必须由家长再次批准新快照；失败返回 `FAILED`、保留最近成功快照和既有批准状态。不同站点的栏目结构可能无法自动识别，由家长更换为该站稳定的栏目首页。

课程版本采用 `DRAFT → PUBLISHED`，发布内容不提供更新/删除入口；新内容创建下一版本，历史 Assignment 始终引用原版本。九类活动分别要求 `VIEWED`、`CHECKED` 或 `PARENT_CONFIRMED`；视频观看本身不等于掌握，客观题答案不下发 Android，亲子阅读、口头和线下实践由家长最终确认。Assignment 使用 `ASSIGNED → IN_PROGRESS → SUBMITTED → COMPLETED`，并支持 `SUBMITTED → REWORK_REQUIRED → IN_PROGRESS`；返工后必须新增一次 Attempt 才可再提交。所有写入带幂等键，提交/审核还使用乐观版本。

幼儿园课程版本在 DRAFT 中增加 `kindergartenAgeBand=SHARED_3_4|TRANSITION_5_6` 和一个或多个 `kindergartenDomains=HEALTH|LANGUAGE|SOCIAL|SCIENCE|ARTS`。发布时服务端逐课节强制：最多 3 个活动、总预计时长不超过 15 分钟、屏幕活动累计不超过 8 分钟、单活动不超过 8 分钟、客观题最多两个选择，并且至少包含一个亲子或离屏活动。非幼儿园课程不得携带上述幼儿园元数据；不满足时返回 400 且版本继续保持 DRAFT。

## 响应与错误

成功与失败统一为 `{data,error,traceId}`。当前错误码：

| HTTP | code | 含义 |
| ---: | --- | --- |
| 400 | `VALIDATION_FAILED` | Bean Validation、缺失/非法幂等键或请求不满足业务前置 |
| 401 | `AUTHENTICATION_REQUIRED` / `AUTHENTICATION_FAILED` | 无有效会话或 PIN 不匹配 |
| 403 | `FORBIDDEN` | 已认证角色不允许该操作 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在或不在会话对象范围内 |
| 409 | `CONFLICT` | 状态、并发或幂等键冲突 |
| 429 | `PIN_LOCKED` | PIN 失败达到阈值，暂时锁定 |

字段、校验、Bearer scheme 和完整响应以 [OpenAPI 3.1](../openapi.yaml) 为机器契约。
