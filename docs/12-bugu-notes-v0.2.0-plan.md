# 布谷笔记 v0.2.0 实施计划

## 目标

- 修正连续 Markdown 编辑器占位文字、光标和中文正文的基线差异；编辑态隐藏语法标记时不改变字形尺寸或文本 offset。
- 让编辑与预览中的列表标记更清楚，并保持编号、无序列表和换行对齐，原始 Markdown 不改写。
- 使用布谷笔记品牌图标和标题图，采用薄荷绿与深棕色体系，保留 DeepSeek 模型和 API 服务说明。
- 版本升至 0.2.0（versionCode 12），保留此前 APK。

## 验收

- 离线构建 `:app:assembleDebug :app:lintDebug`。
- 运行 `tools/regression/editor018/run.sh`，验证原文往返、查找替换/撤销、列表及活动/非活动行 marker 尺寸一致。
- 运行既有适用回归；如设备可用，验证编辑器空白占位、中文输入、换行/空行、亮暗主题及侧栏品牌。
- 发布记录记录自动回归结果与设备验证限制。

## 实际结果

构建和 lint 通过；editor018、lifecycle019、Room migration019 回归通过。模拟器只读启动成功，但 ADB 保持 offline，无法安装运行，设备 UI 检查未完成。
