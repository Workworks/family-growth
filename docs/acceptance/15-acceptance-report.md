# V1 验收报告

状态：`IN_PROGRESS`

Stage 13 已交付 Stage 2–11 的 Android 本机基础体验宽度；Stage 14 已交付公开仓库、稳定签名和 v0.2.0→v0.2.1 两版 Release，远端契约验证通过。安装交互、重启持久化和 App 内覆盖升级仍受设备环境阻塞；Stage 3–10 的生产服务端深度业务仍未建设。

## 当前矩阵

| 范围 | 状态 | 证据 |
| --- | --- | --- |
| 项目立项与设计 | `PASS` | [Stage 1](../stages/stage-1-report.md) |
| 后端工程与基础领域 | `PASS（自动化）` | [Stage 2 证据](../evidence/stage-2/acceptance.json) |
| PostgreSQL 16 迁移 | `PASS` | [Stage 2 证据](../evidence/stage-2/acceptance.json) |
| Android 本地基础业务与五区 UI | `PASS（实现/自动化）` | [Stage 13 证据](../evidence/stage-13/acceptance.json) |
| Android 单测/lint/0.2.1 APK | `PASS（构建）` | [Stage 14 证据](../evidence/stage-14/acceptance.json) |
| Android 安装、启动、旋转 | `BLOCKED` | [BLOCKERS](../BLOCKERS.md) |
| 任务、奖励、钱包、基金、防沉迷 | `PASS（本机基础实现）` | [Stage 13](../stages/stage-13-report.md) |
| Stage 3–10 生产服务端闭环 | `NOT_STARTED` | [Stage 路线图](../stages/stage-roadmap.md) |
| GitHub 两版 Release/digest/同签名 | `PASS（远端）` | [Stage 14](../stages/stage-14-report.md) |
| GitHub Release 真机覆盖升级 | `BLOCKED` | [Stage 14](../stages/stage-14-report.md) |
| V1 端到端总验收 | `NOT_STARTED` | [验收标准](../design/08-acceptance-criteria.md) |

完整 V1 只有在 Stage 3–10 交付并按 [端到端场景](../manuals/scenarios/family-growth-v1-e2e.md)真实回放后才能标记完成。
