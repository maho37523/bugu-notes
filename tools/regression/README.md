# 本地回归检查

当前目标：Android v0.4.0 / Room schema 9。目录名保留历史，不代表只测对应旧版。新会话以 [质量规则](../../docs/quality.md) 与 [实际基线](../../docs/exec-plans/R02-quality-baseline.md) 为准。

由 Codex 在项目根运行 `python3 tools/check.py baseline`：使用全新隔离副本，先构建同一源码再运行当前六项套件，不混用原目录旧生成类。单独命令仅在明确的同源隔离副本执行。

| 命令 | 覆盖与边界 |
| --- | --- |
| `python3 tools/regression/migration019.py` | 真实 6→8 SQL 与新生成 schema 对比，验证列/索引、旧笔记/消息/草稿及来源默认值；旧表是合成重建 |
| `python3 android/tools/regression/test_room_migration_v6.py` | 合成 v5 表执行历史 5→8 SQL、默认值和草稿删除；已修复 schema 8 重复列夹具问题 |
| `bash tools/regression/lifecycle019/run.sh` | 生产 Repository/DAO SQL/实体/上下文/BackupService + 真实 SQLite JDBC/适配器；草稿、首发失败回滚、项目范围、搜索、归档/删除/恢复、合成 v8/7/6 备份、附件路径以及恢复中途失败回滚 |
| `bash tools/regression/editor018/run.sh` | 当前编译编辑器/Markdown 类：原文往返、选区、撤销重做、列表/代码/中文/Unicode/查找替换和 4,000 段落 |
| `bash tools/regression/network021/run.sh` | 离线配置迁移/供应商隔离、请求体、SSE、搜索与来源夹具；不调用真实 API |
| `python3 android/tools/regression/test_backup_zip_codec.py` | 生产 Java ZIP Codec：往返、错误路径、重复条目与大小限制 |

`lifecycle019` 现含 schema 8→9 的真实迁移入口合成执行及 v0.4 目录 scope/旧备份回归，覆盖 schema 8 ZIP 的共享目录拆分、隐藏内容、临时草稿和恢复失败附件保护。2026-11-07 的完整离线基线报告见 `.engineering/reports/20261107-115155-862644/summary.json`。JVM runner 需要现有 JDK 21 和 ~/.gradle 缓存；部分套件读取同次构建生成类/schema。合成旧库/备份不能冒充真实历史用户文件，JDBC 适配器不等于 Android Room 的完整运行环境。设备触摸/中文 IME、进程恢复、真实在线、磁盘耗尽和全部 1→9 仍需后续验证。

`backup018/`、`migration018.py` 是历史 v0.1.8 套件，原样保留，不纳入当前六项通过数。测试夹具不打包到 APK，不读真实 Key 或笔记。

## 隔离 Android 启动与覆盖升级

新增 `android_startup.py` 直接安装并启动 APK，覆盖 Compose 首页渲染、异步 Room 打开、新安装、schema 8 合成资料覆盖升级、内容保留与多次冷启动。必须使用全新、名称以 `BuguSmoke` 开头的可丢弃 AVD；脚本校验 emulator 序号、AVD 名称及虚拟设备属性，默认拒绝已有应用数据。`--reset-synthetic` 只允许清理该专用 AVD 的合成应用。绝不对手机运行，也不复制既有 AVD 数据。

```sh
python3 tools/regression/android_startup.py --adb /Users/cris/Library/Android/sdk/platform-tools/adb --serial emulator-5582 --avd BuguSmoke17 --apk /absolute/candidate.apk --previous /absolute/schema8.apk --report /absolute/new-report-directory
```

报告保存 APK 哈希、Android/API/页大小、启动结果、崩溃缓冲、UI XML、截图与合成资料断言。该检查独立于 `baseline`，不会在离线入口自动启动模拟器；模拟器工具及系统镜像需已具备或单独获准下载。本机模拟器通过不能证明真实 OPPO、真实历史数据、中文 IME 或在线 API 通过。

启动回归可加 `--dangling-draft-folder`，在 schema 8 专用合成库创建全局/项目/孤立项目草稿的已删除目录引用，覆盖此次真机异常。验证升级成功、引用变 null、草稿正文/引用/附件 JSON 与附件文件保留；非当前项目时间戳逐值保留。全局当前草稿有既有启动自动保存行为，不把其 updatedAt 更新误判为迁移篡改；纯迁移全部字段与时间戳由 lifecycle019 精确比较。严格 ZIP/v9 校验及异常事务回滚仍保留。
