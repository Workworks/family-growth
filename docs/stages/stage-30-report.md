# Stage 30：加密 Room、本机迁移与游标同步

状态：`BLOCKED`（可离线工程完成）

产品 Phase：V1 可靠同步 / P1

需求：REQ-013、REQ-021、REQ-051、REQ-052；Backlog P-19、P-20

## 用户可观察目标

升级 App 后，既有本机任务、钱包、学习进度、家长 PIN 哈希和偏好不丢失。本机业务快照不再以 SharedPreferences 明文 JSON 保存，而进入 Room 管理的版本化表，并由 Android Keystore AES-256-GCM 加密。连接家庭服务后，App 以服务端游标和实体摘要拉取变化/删除；多设备版本冲突进入家长可见的“需要合并”列表，不静默覆盖或假装同步成功。

## 范围内

- Room 2.8.4 + KSP，数据库包含加密快照、迁移凭据、同步游标、通用写前队列和冲突记录；业务自由文本只以 AES-GCM 密文落盘。
- 首启迁移采用“读取旧 JSON → 加密写 Room → 解密/解码校验 → 写迁移凭据 → 删除旧 state key”；任何一步失败保留旧数据并返回可恢复错误。BCrypt PIN 哈希继续保留在 App 私有 SharedPreferences，不存明文 PIN。
- 服务端为当前 Android 事实投影提供 cursor/digest 增量协议：孩子资料、任务/Completion、Wallet、学习 Assignment、使用策略和家长待办。客户端提交已知实体摘要，服务端只返回变化实体和 tombstone。
- 每个同步客户端使用不可伪造的设备绑定会话或随机 client ID；服务端保存确认游标，跨家庭/孩子统一 404。known digest 数量、请求体和返回量均有限制。
- 现有乐观版本/幂等冲突继续由源业务裁决；Android 将 409 连同作用域、动作和服务端事实记录到 Room，刷新后由家长显式重试/放弃，不自动 last-write-wins。

## 范围外

- 在 Android 端复制服务端 Wallet/Ledger 作为可离线写的第二事实源、离线发奖、离线确认兑换/基金/兑现、跨家庭合并、后台常驻推送。
- 自制密码学、硬编码密钥、把 Token/PIN 放入 Room、因迁移失败删除旧 SharedPreferences，或用构建通过冒充真实升级保留。

## 安全与儿童行为不变量

1. Keystore 密钥不可导出；每次加密使用唯一 12-byte IV，AES/GCM/NoPadding，密文包含版本与 IV。解密失败不回退明文。
2. Room 不存 Bearer Token、服务端 PIN、答案键或家长私密说明；退出/401 继续清除内存会话。
3. 服务端始终是连接态 Wallet/Ledger/课程/权限事实源；客户端 tombstone 只删除缓存，不删除服务端事实。
4. 冲突只向家长显示中性事实，不让孩子承担合并选择，不以红点、倒计时、损失文案施压。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP30-1 | 已完成 | 本 Spec、数据边界、迁移顺序、游标/tombstone/冲突规则 |
| WP30-2 | 已完成 | Android Room/KSP、Keystore 加密快照与旧 SharedPreferences 可恢复迁移 |
| WP30-3 | 已完成 | Room cursor/outbox/conflict 表、Repository 和迁移/加密测试 |
| WP30-4 | 已完成 | V27 服务端 sync checkpoint 与受限 digest delta API |
| WP30-5 | 已完成 | Android 增量同步、tombstone 应用和 409 冲突中心 |
| WP30-6 | 已完成 | H2/PostgreSQL、Android 双变体、OpenAPI/手册/运维/证据门禁 |
| WP30-7 | 外部阻塞 | 从 v0.3.8 覆盖升级、断网/杀进程/双设备冲突和数据保留真机回放 |

## 验证方式

| ID | 环境 | 操作 | 预期 |
| --- | --- | --- | --- |
| V30-01 | Android 测试 | 旧 JSON 正常/损坏/写失败迁移，重启重复执行 | 成功只迁移一次；失败保留旧数据；无明文业务 JSON |
| V30-02 | Android 测试 | 加密篡改、不同 IV、丢失密钥、schema 升级 | 篡改/丢钥 fail-closed；迁移可回放 |
| V30-03 | H2/PostgreSQL/API | 首次/重复/跨孩子同步，known digest 和 tombstone | cursor 单调；重复零变化；删除显式传播；跨对象 404 |
| V30-04 | Android/JVM | 401、409、断网、重启和显式解决 | 队列/冲突持久保留，不静默覆盖、不重复写 |
| V30-05 | 通用 | 全量、OpenAPI、链接、JSON、secret、diff | 双数据库/双变体和治理门禁通过 |
| V30-06 | 真机 | 0.3.8 覆盖升级、杀进程、双设备编辑 | 真实数据保留和冲突体验通过；缺设备保持 BLOCKED |

## 完成标准

- [x] AC30-01：目标、迁移原子性、加密、同步投影、冲突和非目标已明确。
- [x] AC30-02：Room 加密快照和旧数据迁移顺序通过反向测试。
- [x] AC30-03：cursor/digest/tombstone API 通过 H2/PostgreSQL 和对象隔离。
- [x] AC30-04：Android 增量应用、持久冲突与恢复策略通过 JVM/Lint/双 APK；真实重启回放保持阻塞。
- [x] AC30-05：契约、手册、运维、证据同步；真实覆盖升级缺失保持 BLOCKED。

结构化证据见 [Stage 30 acceptance](../evidence/stage-30/acceptance.json)。
