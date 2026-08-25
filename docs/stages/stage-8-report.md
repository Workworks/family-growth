# Stage 8：Android 生产双端核心页面

状态：`BLOCKED`

产品 Phase：7　需求：REQ-003、REQ-004、REQ-006、REQ-021、REQ-022

## 目标与非目标

用户可观察目标：家长在 Android 平板配置局域网服务、登录并同步家庭/孩子状态；家长端可管理任务、钱包和财商能力，孩子端继续遵守三入口、单任务与家长托底准则，并能处理加载、离线、认证过期和重试。

范围内：HTTPS/受控局域网 URL 校验、会话只驻内存、生产 API client、连接状态、核心数据同步、角色路由、儿童门禁和错误/空态。范围外：公网托管、静默凭据持久化、跨 App 设备管控和绕过系统安装边界。

## 边界与不变量

- 服务端授权是最终边界；客户端隐藏不构成权限。
- Bearer/PIN 不写日志、BuildConfig、证据或明文持久化；断线不伪成功。
- CHILD 仍只有今天/小任务/我的三个一级入口，复杂交易需家长共同操作。
- Android 主线程不执行网络；所有写操作携带独立幂等键。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP8-1 | 已完成 | 生产 API client、URL/错误/会话模型 |
| WP8-2 | 已完成 | 家长连接登录、同步状态和认证过期恢复 |
| WP8-3 | 已完成 | 家长/孩子核心数据映射与适龄路由 |
| WP8-4 | 外部阻塞 | JVM/lint/debug/release 已过；Compose 真机/平板验收缺设备 |

## 完成标准

- [x] AC8-01 PASS（自动化）：HTTPS 与开发版 loopback/私网 HTTP 地址策略通过；生产 manifest 禁止明文，debug overlay 才显式允许。
- [x] AC8-02 PASS（自动化）：父/子 Token 仅由 `MemorySessionStore` 持有；401 单测清空会话并进入 Expired；错误态可重连。
- [x] AC8-03 PASS（代码/API）：受 RBAC 保护的 sync snapshot 返回任务、completion、钱包和今日摘要；孩子提交/家长确认使用随机独立幂等键并回读 snapshot。
- [x] AC8-04 PASS（静态/JVM）：既有 CHILD 三入口与复杂财商隐藏策略未改变；服务端继续拒绝 3–5 岁 CHILD 基金交易。
- [ ] AC8-05 BLOCKED：JVM、lintDebug、assembleDebug、assembleRelease 通过；无 Android 平板，Compose 真实触控/旋转/无障碍无法执行。

## 安全检查、已知限制与交接

家庭局域网生产默认要求可信 HTTPS；仅开发构建可显式使用 loopback/私网 HTTP。证书信任错误不得降级忽略。

实现入口：Android `remote/`、`FamilyAppViewModel`、家长服务连接卡；后端 `Stage8Models/Service/Store/Controller` 的 `/sync` 聚合接口。证据：[Stage 8 acceptance](../evidence/stage-8/acceptance.json)。设备到位后只需回放 AC8-05，不影响 Stage 9 可离线后端推进。
