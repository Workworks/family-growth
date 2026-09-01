# 待办执行账本

**最后更新：2026-09-01。**

本文件只维护**尚未完成且可以继续执行的事项**，按优先级和推进责任排序。历史交付查看 [Stage 路线图](stages/stage-roadmap.md)和各 Stage 报告；缺陷查看 [bugList](bug/bugList.md)；问题查看 [AQ](aq/aq.md)；外部阻塞的详细解除条件查看 [BLOCKERS](BLOCKERS.md)。

状态以 Stage 报告为准。本账本不得把 `BLOCKED` 写成“进行中”，不得保留已完成事项的长篇实施记录。

## 1. 当前重点（按执行顺序）

| 优先级 | Stage | 状态 | 下一动作 | 推进方 | 完成证据 |
| --- | ---: | --- | --- | --- | --- |
| P0 | 全局 | `IN_PROGRESS` | 每轮登记用户新增需求/建议，并同步 AGENTS/codex-skills、Stage、优先级和证据 | Agent | `requirements/requirement-ledger.md` |
| P2 | 31 | `IN_PROGRESS` | 实施储蓄奖励、可复现模拟市场、持有期成本与统一经济实验室 | Agent | Stage 31 Spec/迁移/API/Android/证据 |
| P1 | 30 | `BLOCKED` | 目标平板覆盖升级、重启/断网和两设备冲突回放 | 用户提供设备/可信服务；Agent 回放 | Stage 30 V30-06 |
| P1 | 29 | `BLOCKED` | 两台目标设备与可信 HTTPS 回放配对、撤销、重启和无障碍 | 用户提供设备/服务；Agent 回放 | Stage 29 V29-08 |
| P2 | 32 | `NOT_STARTED` | 完成 Android 工具链和最终治理验收 | Agent | Stage 32 Spec/测试/证据 |
| P0 | 22 | `BLOCKED` | WP22-1–5 已完成；连接目标平板回放年龄带/五领域折页、布置、孩子离屏完成、家长一句观察、TalkBack、旋转、Reduced Motion 和触觉关闭 | 用户提供平板；Agent 回放 | Stage 22 报告、`evidence/stage-22/` |
| P1 | 23 | `BLOCKED` | 可离线工作包已完成；连接目标平板回放小学报告、横竖屏、TalkBack、大字体、Reduced Motion 与数据保留 | 用户提供平板；Agent 回放 | Stage 23 报告、`evidence/stage-23/` |
| P1 | 24 | `BLOCKED` | 可离线工作包已完成；连接目标平板回放横竖屏、大字体、TalkBack、Reduced Motion、触控排序和断网恢复 | 用户提供平板；Agent 回放 | Stage 24 报告、`evidence/stage-24/` |
| P1 | 25 | `BLOCKED` | 可离线工作包已完成；连接目标平板回放研究板、TalkBack、Reduced Motion、断网冲突和数据保留 | 用户提供平板；Agent 回放 | Stage 25 报告、`evidence/stage-25/` |
| P0 | 26 | `BLOCKED` | WP26-1–13 和 v0.3.8/14 已完成；连接目标平板与可信 HTTPS 服务回放首装/覆盖升级、四学段、TalkBack/旋转/大字体/Reduced Motion、触觉、断网和数据保留 | 用户提供设备/服务；Agent 回放 | Stage 26 报告、v0.3.8 Release、`evidence/stage-26/` |
| P1 | 27 | `BLOCKED` | 可离线工程已完成；连接目标平板回放系统相册、JPEG/PNG/WebP 上传、TalkBack、大字体、旋转、Reduced Motion 与重启保留 | 用户提供设备/服务；Agent 回放 | Stage 27 报告、`evidence/stage-27/` |
| P1 | 28 | `BLOCKED` | 可离线工程已完成；连接目标平板回放家庭奖励约定、孩子兑换待回应、家长审批和现实奖励兑现 | 用户提供设备/服务；Agent 回放 | Stage 28 报告、`evidence/stage-28/` |
| P0 | 21 | `BLOCKED` | v0.3.6/12 已发布；在目标平板从 v0.3.5 应用内覆盖升级并回放 AndroidKeyStore 写前队列、杀进程/断网、重新登录、401/409、真实来源/官方播放和 TalkBack/旋转 | 用户提供平板/可访问服务；Agent 回放 | `stages/stage-23-report.md`、`evidence/stage-23/acceptance.json`、v0.3.6 Release |
| P0 | 20 | `BLOCKED` | 用目标平板回放四学段、触觉开关、Reduced Motion、TalkBack、旋转、重启和真实服务同步 | 用户提供设备；Agent 回放 | `stages/stage-20-report.md`、`evidence/stage-20/acceptance.json` |
| P0 | 18 | `BLOCKED` | 在平板用 v0.3.3→v0.3.4 更新链同时测试双视角、视频计时、奖励兴趣和重启保留 | 用户测试；Agent 根据反馈修复 | Stage 18/19 真机证据 |
| P0 | 16 | `BLOCKED` | 在平板覆盖安装 v0.3.3，验证儿童图标及原有数据保留 | 用户提供设备；Agent 回放 | Stage 16/19 真机证据 |
| P0 | 10 | `BLOCKED` | 用平板连接可访问 HTTPS 家庭服务，回放首装、全链路、TalkBack、重启与 v0.2.2→v0.3.0 更新 | 用户提供设备/服务；Agent 回放 | Stage 10 真机证据 |
| P0 | 8 | `BLOCKED` | 平板回放生产登录同步、孩子提交、家长确认、旋转和无障碍；代码/JVM/lint/build 已过 | 用户提供设备；Agent 回放 | Stage 8 真机证据 |
| P0 | 15 | `BLOCKED` | 用 Android 平板回放三入口、单任务、TalkBack、字体放大和温和限时退出 | 用户提供设备；Agent 回放 | Stage 15 真机证据 |
| P0 | 14 | `BLOCKED` | 用 Android 真机/平板安装 0.2.1 release，通过 App 更新至 0.2.2 并验证数据保留/失败路径 | 用户提供设备；Agent 回放 | Stage 14/15 真机证据 |
| P0 | 2 | `BLOCKED` | 连接 Android 真机/平板，或明确授权重装共享 API 34 system image | 用户提供条件；Agent 回放 | 安装、启动、旋转、截图与结构化证据 |
| P1 | 13 | `BLOCKED` | 连接 Android 真机/平板，安装 0.2.0 包并回放任务/钱包/PIN/限时/重启持久化 | 用户提供条件；Agent 回放 | Stage 13 真机截图与结构化证据 |
| P0 | 11 | `BLOCKED` | 稳定 Release 链已完成且 latest 为 v0.3.8；连接 Android 设备回放 App 内检查、下载、校验、系统确认和数据保留 | 用户提供设备；Agent 回放 | 真机覆盖升级证据 |

## 2. 可由 Agent 继续推进

Stage 27–29 的 P-01/02/03/04/05/07/12/14/15/16 可离线工程已完成并转为目标平板阻塞。当前继续 Stage 30–32；真实平板与可信 HTTPS 继续作为独立外部验收，不阻塞后续工程实现。完整拆分见 [教学与项目剩余事项清单](requirements/teaching-and-project-backlog.md)。

## 3. 只能由外部条件解除

| 优先级 | Stage | 缺少条件 |
| --- | ---: | --- |
| P0 | 2 | 启用 USB 调试的 Android 真机/平板，或重装共享 API 34 system image 的明确授权 |
| P1 | 13 | 启用 USB 调试的 Android 真机/平板；可与 Stage 2 同一次回放解除 |
| P0 | 11/14 | 启用 USB 调试的 Android 真机/平板；仓库、稳定签名和 latest v0.3.8 Release 已具备 |
| P0 | 15 | 启用 USB 调试的 Android 平板/真机，用于儿童端触控和无障碍验收 |
| P0 | 18 | Android 平板/真机，用于双视角、视频、奖励兴趣、重启和 v0.3.3→v0.3.4 更新验收 |
| P0 | 20 | Android 平板/真机与可访问测试服务，用于四学段布局、触觉/Reduced Motion/TalkBack、旋转、重启和配置同步验收 |
| P0 | 21 | Android 平板/真机与可访问测试服务，用于 AndroidKeyStore、杀进程/断网、401/409、来源/官方播放和家长孩子 E2E |
| P0 | 8 | 启用 USB 调试的 Android 平板/真机，用于生产登录、同步、写入和恢复验收 |
| P0 | 10 | 启用 USB 调试的 Android 平板/真机，以及设备可访问并信任证书的 HTTPS 家庭服务 |

## 4. 每次任务的维护规则

1. Agent 启动时读取本文件，并将第 1 节与 `stage-current.md` 对齐。
2. 开始新 Stage 前先在报告写清目标、边界、验证方式和编号完成标准。
3. 完成事项后从执行账本删除；历史结论只留在 Stage 报告和证据。
4. Bug、AQ、外部阻塞和需求分别进入各自唯一入口，不在 TODO 复制长篇详情。
5. 优先级变化同步 TODO/current；Stage 状态变化还要同步 BLOCKERS、roadmap 和 Stage 索引。
