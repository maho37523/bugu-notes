# 设计资产入口

M01 整理日期：2026-11-06。详细登记继续维护在 [资产清单](../docs/assets.md)，迁移与验证见 [M01 记录](../docs/exec-plans/M01-design-assets.md)。本目录不是新的运行时资源根目录。

## 分类与引用

| 类别 | 唯一维护位置 | 消费方 / 保留规则 |
| --- | --- | --- |
| 历史设计参考 | [reference/product-map.png](reference/product-map.png)、[reference/core-screens.png](reference/core-screens.png) | 根 README 与历史产品定义；仅移动，字节未变 |
| 原生品牌原图 | [Android brand](../android/app/src/main/assets/brand) | 保留原 assets 路径；发布记录声明为原始素材，生成图具体派生算法未确认 |
| 原生标题 | [bugu_title.png](../android/app/src/main/res/drawable-nodpi/bugu_title.png) | Components.kt 的 DshWordmark 使用 R.drawable.bugu_title |
| 原生图标 | [res](../android/app/src/main/res) 下五个 mipmap 密度目录的 ic_launcher.png | Manifest 使用 @mipmap/ic_launcher，成套保留 |
| Web 图标、示例图和字体 | [prototype/assets](../prototype/assets) | index.html、app.js、styles.css、manifest.webmanifest、sw.js 保留原 URL |
| Web 字体许可 | [OFL.txt](../prototype/assets/fonts/OFL.txt) | 与三个 Montserrat woff2 同行保存；不推定其他图片已获授权 |
| Web 截图 | [prototype/screens](../prototype/screens) | 保留原型 README 引用，不当作当前 Android 截图 |
| 原生配色和字体策略 | [Theme.kt](../android/app/src/main/java/com/dspro/android/ui/Theme.kt) | 配色单一维护位置；正文使用 SansSerif；不复制 token 为另一实现 |
| Web 配色和字体策略 | [styles.css](../prototype/styles.css) | 独立历史原型；Montserrat 不自动迁入 Android |

## 文件元数据与完整性

[SHA256SUMS](SHA256SUMS) 使用项目根相对路径。每次合法修改资产后，先审查内容，再同步哈希；不能只更新哈希掩盖未解释的变化。以下尺寸直接读取 PNG IHDR，不涉及重新编码。

| 实际文件 | 字节数 | PNG 宽×高 |
| --- | ---: | --- |
| [android/app/src/main/assets/brand/bugu_logo_original.png](../android/app/src/main/assets/brand/bugu_logo_original.png) | 642848 | 1254×1254 |
| [android/app/src/main/assets/brand/bugu_title_original.png](../android/app/src/main/assets/brand/bugu_title_original.png) | 479850 | 2172×724 |
| [android/app/src/main/java/com/dspro/android/ui/Theme.kt](../android/app/src/main/java/com/dspro/android/ui/Theme.kt) | 6645 | — |
| [android/app/src/main/res/drawable-nodpi/bugu_title.png](../android/app/src/main/res/drawable-nodpi/bugu_title.png) | 428361 | 1937×450 |
| [android/app/src/main/res/mipmap-hdpi/ic_launcher.png](../android/app/src/main/res/mipmap-hdpi/ic_launcher.png) | 7294 | 72×72 |
| [android/app/src/main/res/mipmap-mdpi/ic_launcher.png](../android/app/src/main/res/mipmap-mdpi/ic_launcher.png) | 4170 | 48×48 |
| [android/app/src/main/res/mipmap-xhdpi/ic_launcher.png](../android/app/src/main/res/mipmap-xhdpi/ic_launcher.png) | 11304 | 96×96 |
| [android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png](../android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png) | 22793 | 144×144 |
| [android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png](../android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png) | 39229 | 192×192 |
| [design/reference/core-screens.png](../design/reference/core-screens.png) | 198246 | 2200×1500 |
| [design/reference/product-map.png](../design/reference/product-map.png) | 478089 | 1600×2020 |
| [prototype/assets/apple-touch-icon.png](../prototype/assets/apple-touch-icon.png) | 9559 | 180×180 |
| [prototype/assets/demo-chart.png](../prototype/assets/demo-chart.png) | 8119 | 480×300 |
| [prototype/assets/fonts/OFL.txt](../prototype/assets/fonts/OFL.txt) | 4400 | — |
| [prototype/assets/fonts/montserrat-light.woff2](../prototype/assets/fonts/montserrat-light.woff2) | 61548 | — |
| [prototype/assets/fonts/montserrat-medium.woff2](../prototype/assets/fonts/montserrat-medium.woff2) | 62148 | — |
| [prototype/assets/fonts/montserrat-regular.woff2](../prototype/assets/fonts/montserrat-regular.woff2) | 62008 | — |
| [prototype/assets/icon-192.png](../prototype/assets/icon-192.png) | 10546 | 192×192 |
| [prototype/assets/icon-512.png](../prototype/assets/icon-512.png) | 47176 | 512×512 |
| [prototype/assets/icon.svg](../prototype/assets/icon.svg) | 616 | — |
| [prototype/screens/chat.png](../prototype/screens/chat.png) | 228242 | 780×1688 |
| [prototype/screens/home.png](../prototype/screens/home.png) | 150795 | 780×1688 |
| [prototype/screens/overview.png](../prototype/screens/overview.png) | 1194108 | 2200×1760 |
| [prototype/screens/voice.png](../prototype/screens/voice.png) | 503953 | 780×1688 |
| [prototype/styles.css](../prototype/styles.css) | 65422 | — |

## 验证与变更边界

在项目根运行：

```sh
shasum -a 256 -c design/SHA256SUMS
```

同时核对文档相对链接与 Web 静态资源引用；Android 资源通过离线 assembleDebug / lintDebug 尝试验证，结果必须区分通过与环境阻塞。没有专用资产自动化回归；现有编辑器回归只验证文本格式等逻辑，不替代视觉验收。

不移动运行时资源、不改资源 ID、不改变颜色或字体、不建立符号链接、不复制原图制造第二维护源。后续若需要改变其他模块接口，停止并单独报告。图片授权来源、原图到图标的生成方法仍未确认。
