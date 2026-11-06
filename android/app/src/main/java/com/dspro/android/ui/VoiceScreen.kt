package com.dspro.android.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class VoiceLine(val mine: Boolean, val text: String)

@Composable
fun VoiceCallOverlay(onEnd: (String) -> Unit) {
    var state by remember { mutableStateOf("connecting") }
    var seconds by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf(listOf<VoiceLine>()) }
    var aiText by remember { mutableStateOf("") }
    var interrupted by remember { mutableStateOf(false) }
    val status = when (state) {
        "connecting" -> "正在连接…"
        "thinking" -> "让我想想…"
        "speaking" -> "正在回答…"
        else -> "我在听…"
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }

    LaunchedEffect(Unit) {
        delay(700)
        state = "listening"
        delay(2100)
        lines = lines + VoiceLine(true, "帮我对比一下这三款耳机的降噪和佩戴")
        delay(650)
        state = "thinking"
        delay(900)
        state = "speaking"
        val answer = "好的，先说结论：通勤选 A，飞行选 B。"
        aiText = ""
        answer.forEach { ch ->
            aiText += ch
            delay(36)
        }
        lines = lines + VoiceLine(false, aiText)
        aiText = ""
        delay(900)
        state = "listening"
    }

    val infinite = rememberInfiniteTransition(label = "voice")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val ringScale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF28372D), Color(0xFF171D19)),
                    center = Offset(540f, 520f),
                    radius = 1400f,
                )
            )
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AI 通话", color = Color(0xFFF9FAFB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                Text(formatTime(seconds), color = Color(0xFF7E8797), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                DshIconButton(Icons.Outlined.Close, "结束通话", { onEnd(buildTranscript(lines, seconds)) }, tint = Color(0xFF98A2B3))
            }

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension * 0.46f * ringScale
                        drawCircle(Color(0x559BD5B2), radius = r, style = Stroke(width = 1.dp.toPx()))
                        drawCircle(Color(0x339BD5B2), radius = r * 0.86f, style = Stroke(width = 1.dp.toPx()))
                    }
                    Canvas(Modifier.size(170.dp)) {
                        val radius = size.minDimension * 0.46f * pulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFDCEFE1),
                                    Color(0xFF9BD5B2),
                                    Color(0xFF557A60),
                                    Color(0xFF2D4535),
                                ),
                                center = Offset(size.width * 0.36f, size.height * 0.3f),
                                radius = radius * 1.2f,
                            ),
                            radius = radius,
                        )
                        drawCircle(Color.White.copy(alpha = 0.08f), radius = radius * 0.82f, style = Stroke(1.dp.toPx()))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(status, color = Color(0xFFF9FAFB), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(5.dp))
                Text(
                    when (state) {
                        "speaking" -> "点按光球可以打断"
                        "listening" -> "直接说话，或点按光球"
                        else -> "可以随时挂断"
                    },
                    color = Color(0xFF7E8797),
                    fontSize = 10.sp,
                )

                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth().heightIn(max = 162.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lines.takeLast(3).forEach { line ->
                        Surface(
                            color = Color.White.copy(alpha = 0.055f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                                Text(
                                    if (line.mine) "你" else "DEEPSEEK",
                                    color = if (line.mine) Color(0xFFB5DCC0) else Color(0xFFA7D8B6),
                                    fontSize = 7.5.sp,
                                    letterSpacing = 0.7.sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(line.text, color = Color(0xFFE5E7EB), fontSize = 11.5.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                    if (aiText.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.055f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(aiText, color = Color(0xFFE5E7EB), fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(11.dp))
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                VoiceControl(
                    icon = if (muted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                    label = "静音",
                    onClick = { muted = !muted },
                )
                VoiceControl(Icons.Outlined.VolumeUp, "扬声器", onClick = {})
                VoiceControl(
                    icon = Icons.Outlined.CallEnd,
                    label = "挂断",
                    danger = true,
                    onClick = { onEnd(buildTranscript(lines, seconds)) },
                )
                VoiceControl(Icons.Outlined.CameraAlt, "相机", onClick = {})
            }
        }

        // Tap orb to interrupt
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state == "speaking") {
                        if (!interrupted) {
                            interrupted = true
                            aiText = ""
                            lines = lines + VoiceLine(true, "等等，先说服帖度。")
                            state = "listening"
                        }
                    }
            )
        }
    }
}

@Composable
private fun VoiceControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(if (danger) 56.dp else 48.dp)
                .clip(CircleShape)
                .background(if (danger) Color(0xFFEF4444) else Color.White.copy(alpha = 0.07f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                label,
                Modifier.size(if (danger) 22.dp else 20.dp),
                tint = if (danger) Color.White else Color(0xFFE5E7EB),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xFF7E8797), fontSize = 8.5.sp, textAlign = TextAlign.Center)
    }
}

private fun formatTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)

private fun buildTranscript(lines: List<VoiceLine>, seconds: Int): String {
    val body = lines.joinToString("\n") { line ->
        if (line.mine) "你：${line.text}" else "我：${line.text}"
    }
    return "**语音通话 · ${formatTime(seconds)}**\n\n$body\n\n> 通话已结束，转写已保存到这条对话。"
}
