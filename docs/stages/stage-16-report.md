# Stage 16：儿童舒适品牌图标与 v0.3.1 交付

状态：`BLOCKED`

日期：2026-08-26

产品 Phase：计划外 P0 儿童体验与发布延续

需求：REQ-023、REQ-024、REQ-013、REQ-018、REQ-021

## 目标与非目标

用户可观察目标：应用内更新在 GitHub/CDN 短暂抖动时会有限重试并说明失败类型，正确 APK 仍需通过来源、大小和 SHA-256 校验；平板桌面上的 Family Growth 图标温和、清楚、适合 3 岁起儿童辨认，并可通过修复后的 GitHub Release 更新链获得 v0.3.1。

范围内：BUG-002 更新下载弱网韧性与错误分类；原创图标概念、视觉基线、Android adaptive/round/legacy/monochrome 资源、Manifest 接入、版本递增、自动化与稳定 Release。范围外：第三方下载镜像、绕过 APK 安全校验、品牌商标注册、市场投放素材、动态图标、系统桌面控制、真实金融视觉和没有设备条件的真机结论。

## 边界与不变量

- 儿童舒适和可识别高于吸睛、点击率与营销冲击；禁止金币、钞票、涨跌箭头、奖杯、倒计时和高刺激荧光色。
- 不含文字，避免低龄儿童识字负担和生成文字失真。
- adaptive icon 关键主体必须在安全区，legacy 密度资源由同一母版确定性派生。
- 继续使用既有稳定 release 证书；私钥、密码和 Secrets 不进入 Git、日志、证据或回复。
- 构建与图像检查不替代真实平板桌面、系统蒙版和覆盖升级验收。

## 工作包

| ID | 状态 | 内容 |
| --- | --- | --- |
| WP16-0 | 完成 | 修复 BUG-002：弱网重试、线程安全进度、失败分类与安全校验回归 |
| WP16-1 | 完成 | 登记需求、品牌图标基线与生成提示词 |
| WP16-2 | 完成 | 生成、检查并固化 1024px 品牌母版 |
| WP16-3 | 完成 | 接入 adaptive/round/legacy/monochrome Android 资源 |
| WP16-4 | 完成 | Android/文档/签名门禁及公开 v0.3.1 Release 下载复验通过 |
| WP16-5 | 阻塞 | 平板桌面蒙版、壁纸对比、首次启动与 v0.3.0→v0.3.1 覆盖升级 |

## 验证方式

| ID | 环境 | 操作 | 预期 | 证据 |
| --- | --- | --- | --- | --- |
| V16-01 | 图像静态检查 | 检查尺寸、alpha、边角、主体覆盖与色彩 | 母版/前景合法，主体在安全区，无禁止元素 | Stage 16 evidence |
| V16-00 | JVM + 真实 Release 探针 | 测试重试/错误分类/来源/大小/digest，并下载 v0.3.0 对照 | 瞬时 IO 可恢复，篡改仍拒绝，远端资产真实校验通过 | Stage 16 evidence |
| V16-02 | Android 构建 | debug/release 单测、lint、assemble | 资源链接、Manifest、代码和两类 APK 通过 | Stage 16 evidence |
| V16-03 | APK 静态检查 | aapt/apksigner 检查版本、图标引用、包名和证书 | v0.3.1/versionCode 7、同一稳定证书 | Stage 16 evidence |
| V16-04 | GitHub | tag workflow、Release asset、digest 与下载复验 | latest 为 v0.3.1，精确 APK 可下载且签名一致 | Stage 16 evidence |
| V16-05 | Android 平板 | 多种桌面蒙版/壁纸与覆盖升级 | 图标清楚舒适、数据保留 | 缺设备则 BLOCKED |

## 完成标准

- [x] AC16-00 `PASS`：BUG-002 已增加 3 次有限 IO 重试、120 秒读取窗口、线程安全进度和分类提示；来源、大小、SHA-256 与系统安装边界未降低。
- [x] AC16-01 `PASS`：REQ-023/024、设计基线、Stage、Bug 与行为日志先于实现建立。
- [x] AC16-02 `PASS`：原创母版使用书本/新芽和低刺激配色，无文字、金融诱导或高唤醒元素；主体缩入中央安全区。
- [x] AC16-03 `PASS`：Android 5 档 legacy、adaptive、round、API 33 monochrome 资源和 Manifest 接入完成。
- [x] AC16-04 `PASS`：debug/release 单测、lint、构建通过；稳定签名 v0.3.1/versionCode 7、包名与证书核验通过。
- [x] AC16-05 `PASS`：公开 v0.3.1 Release、GitHub digest、下载版本/包名和同一证书复验通过。
- [ ] AC16-06 `BLOCKED`：平板桌面视觉与 v0.3.0→v0.3.1 覆盖升级缺真实设备。

## 当前阻塞

尚无可用 Android 平板/真机，WP16-5 不能以生成图或构建替代。全部可离线工程与公开 Release 已完成，Stage 因真实桌面和更新回放缺失保持 `BLOCKED`。

## 当前实施结果

BUG-002 保持安全校验不降级：只有网络/文件 IO 进入最多三次重试，HTTP、来源、大小、digest 等契约错误立即失败；进度按百分比节流并由主线程发布，旧版无原因通用提示改为超时、DNS/连接、TLS 或网络/存储分类。真实 v0.3.0 Release API 元数据和 CDN 重定向探针通过，说明发布资产本身当前有效；旧版用户设备的具体 IOException 无法从通用提示反推。

图标由内置图像生成模式创建透明母版，再由项目脚本确定性缩入 60% adaptive 安全区并派生资源。视觉语义为“圆角小书托起两片新芽”，采用鼠尾草绿、暖米白和少量柔和杏色；蒙版预览见 `evidence/stage-16/icon-preview.png`。

代码提交 `f487430` 已推送并发布 [v0.3.1](https://github.com/Workworks/family-growth/releases/tag/v0.3.1)。workflow `32923187112` 用时 4m21s 成功；远端 APK 为 11,366,435 字节，SHA-256 `30c0b95fa98fdc5dd91222607a941f965eac678e1f8a68c56cd5cd232021d37b`，versionCode 7、包名和稳定证书均通过。旧 v0.3.0 因 BUG-002 位于下载端，应手动安装一次 v0.3.1，之后再使用修复后的应用内更新链。
