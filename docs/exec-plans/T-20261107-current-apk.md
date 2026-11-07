# 现有版本 APK 构建

日期：2026-11-07；状态：待验收（离线构建完成）；负责人：本会话 Codex，结束后停止写入。

用户要求构建 APK 供真机使用，不使用 PR。当前 Android 工作树无差异，源码版本仍为 0.2.1 / versionCode 13；多厂商及内容管理新版本未实施。本任务不暗示其已实现，不安装应用、不替换真实数据或改变应用身份。

构建使用前次全量基线同一源码的全新隔离副本，离线 `:app:assembleDebug :app:lintDebug` 退出 0，六项回归及工具自检通过；报告 `.engineering/reports/20261107-003607-476863/summary.json`，输入哈希见同目录 `inputs.json`。隔离产物复制至 `.engineering/apk/Bugu-Notes-v0.2.1-current-debug.apk`，SHA-256 `b28c626e2f43eb2c4ed1000fb6ed50cf88027c2533069968e3087758b701f045`，与报告一致。`aapt` 核对 applicationId `com.dspro.android`、minSdk 26；`apksigner verify --print-certs` 通过，证书 SHA-256 `b55864e4aaf8cca0f1e27b259c43715d16fbf9b172e500fc540c61dbfad53917`，与仓库历史 v0.2.1 debug APK 一致。实际设备已安装包及签名未检查，覆盖安装、启动清理及用户数据未验证；不要通过卸载旧应用处理签名不匹配。

起始 main / `708a7f0`，保留全部既有未提交文件；本次仅增加本记录、索引项和忽略目录中的 APK。无 Git 提交、推送、安装或后台构建。回退只移除本任务记录、索引行和该 APK；临时隔离目录和忽略产物并非长期备份。
