# 儿童舒适品牌图标基线

## 1. 目标

Family Growth 的 Launcher 图标让 3 岁起儿童感到温和、可靠、容易辨认，同时让家长理解它与家庭陪伴、学习成长有关。图标不是奖励刺激器，也不承担金融产品暗示。

## 2. 视觉语义

- 核心符号：一本圆角打开的小书托起一株新芽，表达“学习、照料、慢慢长大”。
- 情绪：安静、友好、稳定；不使用夸张表情、强胜利姿态、闪烁、爆炸或速度线。
- 色彩：低饱和鼠尾草绿、暖米白与少量柔和杏色；避免大面积纯红、荧光色和高频彩虹。
- 形式：圆润、清楚、少细节；缩小到 48dp 仍可识别，不放文字、水印、金币、钞票、涨跌箭头或真实金融标志。

## 3. Android 交付约束

- adaptive icon 分离温和底色与前景符号，关键内容处于 Android 安全区，圆形、圆角方形等系统蒙版均不切断主体。
- legacy icon 提供 mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi PNG；adaptive icon 至少覆盖 API 26+，monochrome 覆盖 API 33+。
- Manifest 同时声明 `icon` 与 `roundIcon`；不依赖网络或运行时生成。
- 原始生成稿和最终 1024px 品牌母版进入项目，生成式草稿不能直接替代缩放与资源校验。

## 4. 儿童最佳利益检查

图标只帮助儿童识别 App，不通过角色凝视、奖品、稀缺提示或高刺激色彩诱导点击。其成功指标是“能辨认、愿意在家长陪伴下打开、不会造成视觉压力”，不是启动频次或停留时长。

## 5. 验收

1. 视觉审查：主体、轮廓、色彩和禁止元素符合本基线。
2. 静态资源：所有 density、adaptive、round、monochrome 资源可由 Android 构建解析。
3. 自动化：Android 单测、lint、debug/release 构建通过。
4. 真机：目标平板桌面在浅色/深色壁纸、圆形/方形蒙版下清楚舒适；无设备时必须保持阻塞。

## 6. 可重复生成

批准的生成母版保存在 `family-growth-android/branding/`。Android 资源使用以下命令确定性派生；不得用每次重新生成的随机图像直接覆盖已批准母版：

```powershell
python scripts/generate_android_icons.py `
  --source family-growth-android/branding/family-growth-icon-generated-source.png `
  --res family-growth-android/app/src/main/res `
  --branding family-growth-android/branding `
  --preview docs/evidence/stage-16/icon-preview.png
```
