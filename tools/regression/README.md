# 本地回归检查

当前目标：Android v0.2.1 / Room schema 8。目录名保留历史，不代表只测对应旧版。新会话以 [质量规则](../../docs/quality.md) 与 [实际基线](../../docs/exec-plans/R02-quality-baseline.md) 为准。

由 Codex 在项目根运行 `python3 tools/check.py baseline`：使用全新隔离副本，先构建同一源码再运行当前六项套件，不混用原目录旧生成类。单独命令仅在明确的同源隔离副本执行。

| 命令 | 覆盖与边界 |
| --- | --- |
| `python3 tools/regression/migration019.py` | 真实 6→8 SQL 与新生成 schema 对比，验证列/索引、旧笔记/消息/草稿及来源默认值；旧表是合成重建 |
| `python3 android/tools/regression/test_room_migration_v6.py` | 合成 v5 表执行真实 5→8 SQL、默认值和草稿删除；已修复 schema 8 重复列夹具问题 |
| `bash tools/regression/lifecycle019/run.sh` | 生产 Repository/DAO SQL/实体/上下文/BackupService + 真实 SQLite JDBC/适配器；草稿、首发失败回滚、项目范围、搜索、归档/删除/恢复、合成 v8/7/6 备份、附件路径以及恢复中途失败回滚 |
| `bash tools/regression/editor018/run.sh` | 当前编译编辑器/Markdown 类：原文往返、选区、撤销重做、列表/代码/中文/Unicode/查找替换和 4,000 段落 |
| `bash tools/regression/network021/run.sh` | 离线配置迁移/供应商隔离、请求体、SSE、搜索与来源夹具；不调用真实 API |
| `python3 android/tools/regression/test_backup_zip_codec.py` | 生产 Java ZIP Codec：往返、错误路径、重复条目与大小限制 |

JVM runner 需要现有 JDK 21 和 ~/.gradle 缓存；部分套件读取同次构建生成类/schema。合成旧库/备份不能冒充真实历史用户文件，JDBC 适配器不等于 Android Room 的完整运行环境。设备触摸/中文 IME、进程恢复、真实在线、磁盘耗尽和全部 1→8 仍需后续验证。

`backup018/`、`migration018.py` 是历史 v0.1.8 套件，原样保留，不纳入当前六项通过数。测试夹具不打包到 APK，不读真实 Key 或笔记。
