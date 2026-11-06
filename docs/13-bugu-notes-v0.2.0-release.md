# 布谷笔记 v0.2.0 发布记录

## 版本

- Version name：`0.2.0`
- Version code：`12`
- APK：[Bugu-Notes-v0.2.0-debug.apk](../android/Bugu-Notes-v0.2.0-debug.apk)
- SHA-256：`8e9077f9f9573e30dd73f3a808d1f5fcb7a8b647fb8b9a68f6049e58bfe3d57d`
- applicationId 保持 `com.dspro.android`；Room schema 与 API 服务品牌含义未变。

## 更新

- BasicTextField 正文和空白占位共用字号、行高及 font padding 设置。
- 编辑态隐藏 Markdown 标记只将颜色设为透明，不缩小字形；identity offset mapping 与文档原始字符长度保持一致，避免光标/中文输入排版因动态字体尺寸变化漂移。
- 列表、标题、引用保留原始 Markdown 字符；列表 marker 颜色沿用高对比正文色。
- 应用名称、侧栏标题、launcher 图标及备份默认名更新为布谷笔记。原始 logo 与标题素材保存在 `android/app/src/main/assets/brand/`。
- Material 3 与 DshPalette 使用薄荷绿/深棕色，语音界面移除蓝紫装饰；API/模型名称和错误语义保留。

## 验证

- 离线执行 `:app:assembleDebug :app:lintDebug`：通过；lint 0 个错误、4 个警告。
- APK 元数据：`com.dspro.android`，version code `12`，version name `0.2.0`。
- `apksigner verify --verbose --print-certs`：v2 签名校验通过，单个 Android Debug 签名者（证书 SHA-256：`b55864e4aaf8cca0f1e27b259c43715d16fbf9b172e500fc540c61dbfad53917`）。
- `tools/regression/editor018/run.sh`：通过；覆盖原文往返、编辑/撤销、中文 Markdown、活动/非活动列表行长度与标记尺寸、编号和换行。
- `tools/regression/lifecycle019/run.sh`：通过；笔记/会话生命周期、共享文件夹搜索及备份兼容。
- `python3 tools/regression/migration019.py`：通过；Room 6→7 数据迁移与 schema 对照。
- 模拟器只读启动成功，但 ADB 长时间保持 offline，无法安装和运行 APK；因此未进行设备上的视觉、IME 和亮暗主题检查。
