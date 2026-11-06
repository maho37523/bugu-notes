# ADR-0001：完整业务备份排除 API 配置

- 状态：现有实现，补录。
- 补录日期：2026-11-06。
- 原始决策日期及决策者：未确认。

## 背景与证据

[BackupService](../../android/app/src/main/java/com/dspro/android/data/notes/BackupService.kt) 明确声明 API 配置不属于备份，并枚举业务表和附件；[ApiConfigStore](../../android/app/src/main/java/com/dspro/android/data/ApiConfig.kt) 将连接设置放在偏好存储中；[Android README](../../android/README.md) 明确说明 API Key 与模型连接设置不会从完整备份导入。

## 实际决策

应用自有完整业务备份包含笔记、对话、项目、记忆、版本、草稿及附件，不包含 API Key 和模型连接设置。恢复业务备份不导入这些配置。

## 影响与边界

业务归档可与连接凭据分开管理；跨设备恢复后仍需检查或配置连接。后续修改备份时必须维持此边界，除非另行批准并记录变更。

此结论只针对应用自有 BackupService，不意味着归档加密，也不保证 Android 系统备份排除偏好；Manifest 的 allowBackup 与明文回退风险见 [当前状态](../current-state.md)。不对历史方案比较作无证据推断。
