# R02：可重现质量基线

日期：2026-11-06。状态：完成（本机离线基线范围）。负责人/集成人：本轮 Codex；不改业务源码、依赖或设计；集成与交接文档完成后结束写入。

授权沿用用户本次工程基础实施要求。起始本地源码提交 `8ea1938`；先复现当前工具和回归，禁止混用旧生成类。构建不安装应用、不访问真实数据，网络测试仅离线夹具。

## 环境与步骤

- 本机 Python 3.14.7，JDK `/opt/homebrew/opt/openjdk@21`，实际版本 21.0.12.1；SDK 35、Build Tools 35.0.1 和 Gradle 8.9 离线分发已存在。
- 从已核对 Git 工作文件复制源码到隔离目录，不复制历史 APK、local.properties、build/.gradle/.kotlin、真实数据或签名文件。位置记录于项目内 `.engineering/reports/R02-20261106/context.json`。
- 在副本 android 目录运行 `./gradlew --offline --no-daemon :app:assembleDebug :app:lintDebug`，显式使用上述 JDK 与本机 SDK。
- 使用该副本的生成类/schema 跑 quality 中六项当前回归；先记录旧迁移脚本真实失败，再作测试夹具的最小修复，不删断言。
- 最后核对生产源码/设计保护、记录报告和 APK 校验；设备、在线和完整 1→8 升级不能据此宣布通过。

## 进度与证据

| 检查 | 当前结果 |
| --- | --- |
| R01 Git 完整性与导出 | git fsck 通过；从初始提交实际流式导出 171 文件，逐项与磁盘哈希一致 |
| 构建与 Lint | 通过，43 秒、48 个任务；0 errors / 4 warnings，与历史 M01 四类警告一致；未修复或屏蔽既有警告 |
| 原始六项回归 | migration/lifecycle/editor/network/ZIP 五项通过；legacy_migration 退出 1，实际复现 duplicate column name: sourcesJson，报告保存 original-regressions.json 和各项 original.log |
| 测试修复后定向检查 | legacy_migration 与扩展的 lifecycle 均通过；updated-regressions.json 记录退出码和时间 |
| 统一入口集成验证 | `python3 tools/check.py baseline` 退出 0：全新副本构建、Lint、自检与六项回归全部通过，详见下表 |

已定位隔离目录 `/private/tmp/bugu-foundation-r9ly9xxx`；报告目录 `.engineering/reports/R02-20261106/`。构建日志 build.log，Lint 详细报告在隔离副本 android/app/build/reports。四个警告是 Sidebar 参数顺序和依赖 BouncyCastle 的三条 TLS 信任管理器提示，运行时可达性未确认，不能据此宣布应用关闭 TLS 验证。

## 最终集成执行证据

由项目根运行 `python3 tools/check.py baseline`，报告 `.engineering/reports/20261106-222123-115808/summary.json`，精确工作文件哈希见同目录 inputs.json；起始 HEAD 为 8ea1938，包含本次未提交的测试/检查工具，不能将 HEAD 单独当作执行源码。隔离副本为 `/var/folders/yx/j5kr4x4x6pgdh00c0kgrjbmr0000gn/T/bugu-baseline-96nmq6hp`。执行过程中输入内容未变；之后只更新工程结果文档并重新做 quick 检查，不重复构建未改变的生产代码。

| 实际步骤 | 退出码 / 耗时 | 结果 |
| --- | --- | --- |
| 检查工具自检 unittest | 0 / 0.06 秒 | 10 项通过，含失败/缺环境不能记为通过的负例 |
| offline assembleDebug + lintDebug | 0 / 44.88 秒 | 48 任务，Lint 0 errors / 4 warnings |
| migration019 | 0 / 0.05 秒 | 真实 6→8 SQL、schema/索引和合成旧记录 |
| lifecycle019 | 0 / 5.37 秒 | 真实 Repository/DAO + SQLite JDBC；包含恢复 SQL 失败回滚/附件保护 |
| editor018 | 0 / 6.07 秒 | 当前编译类、往返/选区/撤销/Unicode/长文等逻辑 |
| network021 | 0 / 2.83 秒 | 仅离线夹具，供应商隔离/请求/SSE/搜索 |
| ZIP Codec | 0 / 0.53 秒 | 往返、非法路径、重复与大小边界 |
| test_room_migration_v6 | 0 / 0.02 秒 | 修复后的合成 v5→8、草稿删除与默认值 |

所有步骤均实际执行，无跳过。逐项日志及 Lint TXT/HTML 已保存在上述本机报告目录；关键结果如本表持久化，不仅留在临时目录。

新 APK SHA-256：`b28c626e2f43eb2c4ed1000fb6ed50cf88027c2533069968e3087758b701f045`。额外实际执行现有 SDK 的 `apksigner verify --verbose --print-certs` 与 `aapt dump badging`：新包和历史 v0.2.1 均验证通过，applicationId `com.dspro.android`、versionName 0.2.1 / code 13 相同，公开证书 SHA-256 均为 `b55864e4aaf8cca0f1e27b259c43715d16fbf9b172e500fc540c61dbfad53917`；仅确认既有 debug 签名连续，不声称正式渠道签名已建立。未读取或输出私钥。

新 APK 内 assets/brand 的两张原图与源码逐字节一致。额外产物检查记录为同报告目录 artifact-verification.json / signature.txt；该额外验签本次单独执行，尚未自动纳入 baseline 入口。历史 APK 原件不覆盖，新旧 APK 哈希不同不推定功能不同或严格字节重现。

## 实际限制与下一步

生产源码、构建配置、依赖及所有既有设计/APK 与工程启动前校验一致；非文档既有文件仅两个已授权回归文件改变。没有安装/启动应用、真实迁移/恢复或在线调用。全部 1→8、真实旧库、设备 IME/进程恢复、存储耗尽、TLS 可达性及性能仍未确认；正式发布前应按独立任务补齐。当前本机离线基线已可由新会话重复执行。

## 回退与交接

所有生成产物仅在隔离副本；报告在忽略目录 .engineering/reports。测试文件如修改，保留 Git 初始版并重新验证；不以覆盖生产数据作回退。阶段结束在本文件保存必要摘要，临时路径不作为唯一证据。
