# Stage 14：公开 GitHub 仓库、分阶段提交与 Release 更新链

状态：`IN_PROGRESS`

日期：2026-08-25

产品 Phase：计划外 P0 发布能力；延续 Stage 11

需求：REQ-019、REQ-018、REQ-013

## 目标与非目标

目标：使用已认证的 GitHub CLI 在账号 `Workworks` 下创建公开仓库 `family-growth`；把当前首次未提交工作树按 Stage 归属组织为可追溯 Conventional Commits 并推送 `main`；将 GitHub Release 更新提升为当前最高优先级，建立可使用稳定 release 签名发布 APK 的真实远端链路。

非目标：不把 debug 证书冒充正式签名；不把 keystore、密码、Token 或儿童数据提交到 Git/GitHub；不在没有 Android 设备时宣称覆盖升级已完成；不重写当前不存在的历史 commit，也不伪造每个中间 commit 都曾独立运行通过。

## 边界与不变量

- 仓库可见性固定为 `public`，名称默认 `Workworks/family-growth`；若 GitHub 已占用则停止并报告，不擅自创建不同名称。
- 当前 `main` 无 commit，全部文件最初均未跟踪。首次历史按实际 Stage 归属精准暂存；共享文件归入最后实际修改它的 Stage，并在证据说明初始历史分组不是时间机器式源码复原。
- `dist/*.apk` 不进入 Git 历史，APK 只作为 GitHub Release asset 或本地交付物；仓库保留文字说明、哈希与可重建流程。
- 发布必须依赖 GitHub Secrets 中的稳定 keystore 与口令。若用户尚未提供/授权生成并安全备份，仓库和提交可以完成，但正式 Release 保持 `BLOCKED`。
- App 只接受配置仓库的 GitHub HTTPS Release 资产，保持 SemVer、固定文件名、大小、SHA-256、applicationId、versionCode、签名和系统安装确认边界。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP14-1 | 完成 | 登记 P0 需求、创建发布 Spec、确认 gh 登录与仓库名可用 |
| WP14-2 | 完成 | 审计 `.gitignore`、敏感信息、大文件与公开范围 |
| WP14-3 | 完成 | 按 Stage 组织本地 Conventional Commits |
| WP14-4 | 完成 | `gh repo create --public`、配置 `origin` 并推送 `main` |
| WP14-5 | 进行中 | 用户已授权生成签名并指定 `E:\FamilyGrowthSigningBackup`；生成/备份、配置 Secrets、构建并创建真实 GitHub Release |
| WP14-6 | 部分完成 | 远端可见性和更新源绑定已验证；Release asset/digest 与真机更新待签名/设备 |

## Git 提交分组

| 顺序 | 计划提交 | 主要范围 |
| ---: | --- | --- |
| 1 | `feat(stage-2): add backend and Android foundation` | Maven/Java 后端骨架、数据库迁移与基础工程 |
| 2 | `feat(stage-11): add GitHub Release updater` | 更新客户端、FileProvider、发布 workflow 与发布操作文档 |
| 3 | `feat(stage-13): add local family experience and responsive UI` | Android 本地业务、五区 UI、测试与 0.2.0 交付说明 |
| 4 | `docs(stage-12): align repository governance` | AGENTS、docs 治理、设计、需求、Stage 与证据事实源 |
| 5 | `chore(stage-14): publish public release baseline` | P0 发布优先级、公开仓库配置、Release 证据和后续签名配置 |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V14-01 | 本地 Git | 查看 log/status 与每个 commit 文件清单 | main 有分阶段提交，工作树无意外文件 | Stage 14 evidence |
| V14-02 | Secret scan | 扫描 tracked 文件、忽略 keystore/env/local 配置 | 无凭据、私钥或儿童数据公开 | Stage 14 evidence |
| V14-03 | GitHub CLI/API | 查看仓库元数据和默认分支 | `Workworks/family-growth` 为 PUBLIC，main 已推送 | Stage 14 evidence |
| V14-04 | Android/CI | 使用稳定签名构建 Release 并创建 tag/Release | 精确 APK 资产存在且 GitHub 提供 sha256 digest | 缺签名材料则 BLOCKED |
| V14-05 | Android 设备 | 从低版检查并覆盖升级到高版 | 同签名、版本递增、系统确认、数据保留 | 缺设备则 BLOCKED |

## 完成标准

- [x] AC14-01 `PASS`：热更新已提升为 P0，REQ/Stage/TODO/current 对齐。
- [x] AC14-02 `PASS`：公开前 141 个候选文件通过 secret/大文件/忽略规则审计，APK 未进入 Git history。
- [x] AC14-03 `PASS`：当前项目形成 Stage 2/11/13/12/14 五个 Conventional Commits，清单见证据。
- [x] AC14-04 `PASS`：`Workworks/family-growth` 已由 GitHub CLI 创建为 PUBLIC，main 推送成功。
- [ ] AC14-05 Android 构建配置指向该仓库，稳定签名 Release APK 与 digest 可由公开 Release API 读取。
- [ ] AC14-06 真机完成同签名覆盖升级和数据保留；缺设备时必须 `BLOCKED`。

## 当前实施结果

- GitHub 仓库：[Workworks/family-growth](https://github.com/Workworks/family-growth)，可见性 `PUBLIC`，默认分支 `main`。
- 初始提交为 `26a37f5`（Stage 2）、`99fdb02`（Stage 11）、`3f99f8b`（Stage 13）、`818a603`（Stage 12）、`a84b578`（Stage 14）；后续 Stage 14 配置/证据另有收口提交。
- Android 默认更新源已绑定 `Workworks/family-growth`，仍允许 `GITHUB_REPOSITORY` 构建参数或环境变量覆盖。
- 绑定仓库后的 0.2.0 debug APK 已重新执行 10 项单测、lint、assemble、aapt 和 apksigner；它用于内部验证，不作为正式 Release。
- 授权前 `gh secret list` 为空、`releases/latest` 返回 404；授权后已生成独立 release 身份并配置四项 GitHub Secrets，本地 release 全门禁与证书指纹比对通过。

## 当前阻塞

1. 新签名已生成：私钥放 E 盘，恢复密码单独放当前 Windows 用户目录；两目录 ACL 均只允许当前 Windows 身份完全控制，GitHub 四项 Secrets 已配置。
2. 当前正在通过 tag workflow 发布同签名基线版与更新版；之后仍需要 Android 真机/平板完成覆盖升级和数据保留验收。

公开仓库和分阶段提交已经完成；稳定签名授权已取得，Stage 14 恢复 `IN_PROGRESS`。不得用 debug Release 绕过签名门禁。

## 安全检查、已知限制与交接

GitHub CLI Token 只由系统 keyring 使用，不输出到证据。稳定 release keystore 是未来所有覆盖升级的身份根；若遗失将无法更新已安装 App，因此必须由用户确认备份位置或提供现有证书。debug APK 可以保留为内部测试，但不能充当生产更新链。
