# 待办执行账本

**最后更新：2026-08-25。**

本文件只维护**尚未完成且可以继续执行的事项**，按优先级和推进责任排序。历史交付查看 [Stage 路线图](stages/stage-roadmap.md)和各 Stage 报告；缺陷查看 [bugList](bug/bugList.md)；问题查看 [AQ](aq/aq.md)；外部阻塞的详细解除条件查看 [BLOCKERS](BLOCKERS.md)。

状态以 Stage 报告为准。本账本不得把 `BLOCKED` 写成“进行中”，不得保留已完成事项的长篇实施记录。

## 1. 当前重点（按执行顺序）

| 优先级 | Stage | 状态 | 下一动作 | 推进方 | 完成证据 |
| --- | ---: | --- | --- | --- | --- |
| P0 | 全局 | `IN_PROGRESS` | 每轮登记用户新增需求/建议，并同步 AGENTS/codex-skills、Stage、优先级和证据 | Agent | `requirements/requirement-ledger.md` |
| P0 | 5 | `IN_PROGRESS` | 完成 GiftMoney、版本化双向兑换、透明费用、预算、重放与 PostgreSQL 门禁 | Agent | Stage 5 Spec/证据/commit |
| P0 | 15 | `BLOCKED` | 用 Android 平板回放三入口、单任务、TalkBack、字体放大和温和限时退出 | 用户提供设备；Agent 回放 | Stage 15 真机证据 |
| P0 | 14 | `BLOCKED` | 用 Android 真机/平板安装 0.2.0 release，通过 App 更新至 0.2.1并验证数据保留/失败路径 | 用户提供设备；Agent 回放 | Stage 14 真机证据 |
| P0 | 2 | `BLOCKED` | 连接 Android 真机/平板，或明确授权重装共享 API 34 system image | 用户提供条件；Agent 回放 | 安装、启动、旋转、截图与结构化证据 |
| P1 | 13 | `BLOCKED` | 连接 Android 真机/平板，安装 0.2.0 包并回放任务/钱包/PIN/限时/重启持久化 | 用户提供条件；Agent 回放 | Stage 13 真机截图与结构化证据 |
| P0 | 11 | `BLOCKED` | v0.2.0/0.2.1 两版 Release 已完成；连接 Android 设备回放 App 内检查、下载、校验、系统确认和数据保留 | 用户提供设备；Agent 回放 | 真机覆盖升级证据 |

## 2. 可由 Agent 继续推进

| 顺序 | Stage | 主题 | 前置条件 |
| ---: | ---: | --- | --- |
| 1 | 6–7 | 商店储蓄愿望、纯模拟基金 | Stage 5 完成后按 Spec 顺序推进 |
| 2 | 5 | 恢复 GiftMoney 与兑换闭环 | Stage 15 可离线门禁完成后恢复 |
| 3 | 8–10 | 服务端联调、深度定制与 V1 总验收 | 需要前序后端闭环、部署环境和 Android 设备 |

## 3. 只能由外部条件解除

| 优先级 | Stage | 缺少条件 |
| --- | ---: | --- |
| P0 | 2 | 启用 USB 调试的 Android 真机/平板，或重装共享 API 34 system image 的明确授权 |
| P1 | 13 | 启用 USB 调试的 Android 真机/平板；可与 Stage 2 同一次回放解除 |
| P0 | 11/14 | 启用 USB 调试的 Android 真机/平板；仓库、签名和两版 Release 已具备 |
| P0 | 15 | 启用 USB 调试的 Android 平板/真机，用于儿童端触控和无障碍验收 |

## 4. 每次任务的维护规则

1. Agent 启动时读取本文件，并将第 1 节与 `stage-current.md` 对齐。
2. 开始新 Stage 前先在报告写清目标、边界、验证方式和编号完成标准。
3. 完成事项后从执行账本删除；历史结论只留在 Stage 报告和证据。
4. Bug、AQ、外部阻塞和需求分别进入各自唯一入口，不在 TODO 复制长篇详情。
5. 优先级变化同步 TODO/current；Stage 状态变化还要同步 BLOCKERS、roadmap 和 Stage 索引。
