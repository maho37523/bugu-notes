package com.dspro.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.AutomationTask
import com.dspro.android.data.PluginInfo

@Composable
fun SimpleTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onMenu: (() -> Unit)? = null,
    action: @Composable () -> Unit = {},
) {
    val c = Dsh.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (onMenu != null) {
                DshIconButton(Icons.Outlined.Menu, "菜单", onMenu, size = 32.dp, iconSize = 20.dp)
            } else if (onBack != null) {
                DshIconButton(Icons.Outlined.ChevronLeft, "返回", onBack, size = 32.dp, iconSize = 19.dp)
            }
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            action()
        }
        DshHairline()
    }
}

@Composable
fun TasksScreen(
    tasks: List<AutomationTask>,
    onToggle: (AutomationTask) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
    onMenu: (() -> Unit)?,
) {
    val c = Dsh.colors
    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar("自动化任务", onBack = onBack, onMenu = onMenu, action = {
            DshPill(
                text = "新建",
                leading = { Icon(Icons.Outlined.Add, null, Modifier.size(12.dp), tint = c.accent) },
                onClick = onNew,
            )
        })
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 780.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("定时任务", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
                    Spacer(Modifier.height(4.dp))
                    Text("用一句话创建，到点自动执行。", color = c.muted, fontSize = 11.5.sp)
                    Spacer(Modifier.height(16.dp))
                }
                items(tasks, key = { it.id }) { task ->
                    TaskCard(task, onToggle = { onToggle(task) })
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = c.chipBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, c.hairline),
                        modifier = Modifier.fillMaxWidth().clickable { onNew() },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = c.muted)
                            Spacer(Modifier.width(9.dp))
                            Text("新建一个任务", color = c.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: AutomationTask, onToggle: () -> Unit) {
    val c = Dsh.colors
    Surface(
        color = c.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, c.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(c.chipBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (task.active) Icons.Outlined.Schedule else Icons.Outlined.PauseCircle,
                        null,
                        Modifier.size(17.dp),
                        tint = if (task.active) c.accent else c.subtle,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(task.schedule, color = c.muted, fontSize = 10.5.sp)
                }
                Switch(
                    checked = task.active,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = c.accent,
                        uncheckedThumbColor = c.subtle,
                        uncheckedTrackColor = c.chipBg,
                        uncheckedBorderColor = c.hairlineStrong,
                    ),
                    modifier = Modifier.height(24.dp),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(task.prompt, color = c.muted, fontSize = 11.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (task.active) Icons.Outlined.CheckCircle else Icons.Outlined.PauseCircle, null, Modifier.size(12.dp), tint = c.subtle)
                Text(task.lastRun, color = c.subtle, fontSize = 9.5.sp)
                Spacer(Modifier.weight(1f))
                Text(if (task.active) "进行中" else "已暂停", color = if (task.active) c.accent else c.subtle, fontSize = 9.5.sp)
            }
        }
    }
}

@Composable
fun PluginsScreen(
    plugins: List<PluginInfo>,
    onToggle: (PluginInfo) -> Unit,
    onBack: () -> Unit,
    onMenu: (() -> Unit)?,
) {
    val c = Dsh.colors
    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar("插件", onBack = onBack, onMenu = onMenu)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 780.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item {
                    Text("插件", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
                    Spacer(Modifier.height(4.dp))
                    Text("连接数据与工具，让回答有据可查。", color = c.muted, fontSize = 11.5.sp)
                    Spacer(Modifier.height(16.dp))
                }
                items(plugins, key = { it.id }) { plugin ->
                    Surface(
                        color = c.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, c.hairline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(c.chipBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(pluginIcon(plugin.icon), null, Modifier.size(17.dp), tint = c.muted)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(plugin.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(plugin.description, color = c.muted, fontSize = 10.5.sp, lineHeight = 16.sp)
                            }
                            Switch(
                                checked = plugin.enabled,
                                onCheckedChange = { onToggle(plugin) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = c.accent,
                                    uncheckedThumbColor = c.subtle,
                                    uncheckedTrackColor = c.chipBg,
                                    uncheckedBorderColor = c.hairlineStrong,
                                ),
                                modifier = Modifier.height(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pluginIcon(name: String): ImageVector = when (name) {
    "file" -> Icons.Outlined.Description
    "globe" -> Icons.Outlined.Language
    "calendar" -> Icons.Outlined.CalendarMonth
    "code" -> Icons.Outlined.Code
    else -> Icons.Outlined.Extension
}

@Composable
fun SettingsScreen(
    dark: Boolean,
    onToggleDark: () -> Unit,
    memory: Boolean,
    onToggleMemory: () -> Unit,
    onMemory: () -> Unit,
    onGlobalMemory: () -> Unit,
    inProject: Boolean,
    onApi: () -> Unit,
    onBackupExport: () -> Unit,
    onBackupImport: () -> Unit,
    onMarkdownImport: () -> Unit,
    onArchived: () -> Unit,
    onTrash: () -> Unit,
    versionName: String,
    privacyDisclosure: String,
    onBack: () -> Unit,
    onMenu: (() -> Unit)?,
) {
    val c = Dsh.colors
    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar("设置", onBack = onBack, onMenu = onMenu)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 780.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            ) {
                item {
                    Text("设置", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
                    Spacer(Modifier.height(18.dp))
                    SectionLabel("记忆")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "全局记忆",
                            subtitle = "用于未分类中的对话",
                            trailing = { Icon(Icons.Outlined.ChevronRight, null, Modifier.size(13.dp), tint = c.subtle) },
                            onClick = onGlobalMemory,
                        )
                        if (inProject) SettingsRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "项目记忆",
                            subtitle = "仅用于当前项目",
                            onClick = onMemory,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("API 与模型")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Outlined.Key,
                            title = "API 设置",
                            subtitle = "配置后才能请求真实模型",
                            trailing = { Icon(Icons.Outlined.ChevronRight, null, Modifier.size(13.dp), tint = c.subtle) },
                            onClick = onApi,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("外观")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Outlined.Settings,
                            title = "深色模式",
                            subtitle = "跟随 DSH 的夜间配色",
                            trailing = {
                                Switch(
                                    checked = dark,
                                    onCheckedChange = { onToggleDark() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = c.accent,
                                        uncheckedThumbColor = c.subtle,
                                        uncheckedTrackColor = c.chipBg,
                                        uncheckedBorderColor = c.hairlineStrong,
                                    ),
                                    modifier = Modifier.height(24.dp),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("内容与备份")
                    SettingsGroup {
                        SettingsRow(Icons.Outlined.Archive, "导出完整备份", "包含笔记、对话、记忆、附件与草稿", onClick = onBackupExport)
                        SettingsRow(Icons.Outlined.Description, "导入备份", "查看备份内容后，再确认替换当前数据", onClick = onBackupImport)
                        SettingsRow(Icons.Outlined.Description, "导入 Markdown 文件", "保存到当前工作区和选中文件夹", onClick = onMarkdownImport)
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("对话")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Outlined.SmartToy,
                            title = "记忆",
                            subtitle = "跨对话记住偏好与项目",
                            trailing = {
                                Switch(
                                    checked = memory,
                                    onCheckedChange = { onToggleMemory() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = c.accent,
                                        uncheckedThumbColor = c.subtle,
                                        uncheckedTrackColor = c.chipBg,
                                        uncheckedBorderColor = c.hairlineStrong,
                                    ),
                                    modifier = Modifier.height(24.dp),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("关于")
                    SettingsGroup {
                        SettingsRow(icon = Icons.Outlined.Description, title = "版本", subtitle = "$versionName · Android", trailing = {
                            Text("2026-11-05", color = c.subtle, fontSize = 9.5.sp)
                        })
                        SettingsRow(icon = Icons.Outlined.MoreHoriz, title = "隐私与数据", subtitle = privacyDisclosure, trailing = {
                            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(13.dp), tint = c.subtle)
                        })
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("布谷笔记 · AI 笔记与对话", color = c.muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Dsh.colors.subtle,
        fontSize = 9.sp,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(start = 3.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val c = Dsh.colors
    Surface(
        color = c.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, c.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    val c = Dsh.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(17.dp), tint = c.muted)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp)
            if (subtitle != null) {
                Text(subtitle, color = c.subtle, fontSize = 9.5.sp, lineHeight = 14.sp)
            }
        }
        trailing()
    }
}
