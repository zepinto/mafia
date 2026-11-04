package com.zepinto.mafia

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Ink = Color(0xFF0B0A12)
    val Night = Color(0xFF1B1830)
    val Dusk = Color(0xFF3A2747)
    val Dawn = Color(0xFFE9A15B)
    val Blood = Color(0xFFE5383B)
    val Moon = Color(0xFFF4E9C1)
    val Gold = Color(0xFFFFC857)
    val Mint = Color(0xFF6EE7B7)
    val Aqua = Color(0xFF5CE1E6)
    val Lilac = Color(0xFFB69CFF)
    val avatars = listOf(Gold, Aqua, Mint, Lilac, Color(0xFFFF8A5C), Color(0xFFFF7AB6))
    fun avatar(index: Int) = avatars[index % avatars.size]

    fun role(role: Role) = when (role) {
        Role.MAFIA -> Blood
        Role.DETECTIVE -> Aqua
        Role.DOCTOR -> Mint
        Role.VILLAGER -> Gold
    }
}

val Pill = RoundedCornerShape(50)
val Soft = RoundedCornerShape(22.dp)

fun roleEmoji(role: Role) = when (role) {
    Role.MAFIA -> "🎩"
    Role.DETECTIVE -> "🔍"
    Role.DOCTOR -> "💊"
    Role.VILLAGER -> "🏡"
}

/** The town at night: deep violet with a moon glowing in the top corner. */
fun Modifier.nightBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Palette.Ink, Palette.Night)))
    .drawBehind {
        drawCircle(Brush.radialGradient(listOf(Palette.Moon.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.04f), radius = size.width * 0.5f), radius = size.width * 0.5f, center = Offset(size.width * 0.9f, size.height * 0.04f))
        drawCircle(Brush.radialGradient(listOf(Palette.Blood.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width * 0.05f, size.height * 0.98f), radius = size.width * 0.7f), radius = size.width * 0.7f, center = Offset(size.width * 0.05f, size.height * 0.98f))
    }

/** The town by day: dusk violet warming to dawn at the bottom. */
fun Modifier.dayBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Palette.Night, Palette.Dusk, Color(0xFF7A4A4A))))
    .drawBehind {
        drawCircle(Brush.radialGradient(listOf(Palette.Dawn.copy(alpha = 0.35f), Color.Transparent), center = Offset(size.width * 0.5f, size.height * 1.02f), radius = size.width * 0.9f), radius = size.width * 0.9f, center = Offset(size.width * 0.5f, size.height * 1.02f))
    }

@Composable
fun Avatar(name: String, index: Int, size: Dp = 44.dp, dead: Boolean = false) {
    Box(
        Modifier.size(size).background(if (dead) Color.White.copy(alpha = 0.18f) else Palette.avatar(index), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (dead) "✝" else name.firstOrNull()?.uppercase() ?: "?",
            color = if (dead) Color.White.copy(alpha = 0.7f) else Palette.Ink,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.46f).sp,
        )
    }
}

/** The game's mark: a fedora with a red band, bobbing gently. */
@Composable
fun Hat(modifier: Modifier = Modifier, bob: Boolean = true, color: Color = Color.White, band: Color = Palette.Blood) {
    val transition = rememberInfiniteTransition(label = "hat")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (bob) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lift",
    )
    Canvas(modifier) {
        val s = size.minDimension
        translate(top = -lift * s * 0.04f) {
            // the crown, pinched in at the top
            val crown = Path().apply {
                moveTo(s * 0.24f, s * 0.64f)
                cubicTo(s * 0.24f, s * 0.42f, s * 0.26f, s * 0.26f, s * 0.36f, s * 0.24f)
                cubicTo(s * 0.44f, s * 0.22f, s * 0.47f, s * 0.31f, s * 0.5f, s * 0.31f)
                cubicTo(s * 0.53f, s * 0.31f, s * 0.56f, s * 0.22f, s * 0.64f, s * 0.24f)
                cubicTo(s * 0.74f, s * 0.26f, s * 0.76f, s * 0.42f, s * 0.76f, s * 0.64f)
                close()
            }
            drawPath(crown, color)
            drawRoundRect(band, Offset(s * 0.235f, s * 0.53f), Size(s * 0.53f, s * 0.1f), CornerRadius(s * 0.02f))
            // the brim, curled up at both ends
            val brim = Path().apply {
                moveTo(s * 0.04f, s * 0.6f)
                cubicTo(s * 0.2f, s * 0.7f, s * 0.8f, s * 0.7f, s * 0.96f, s * 0.6f)
                cubicTo(s * 0.92f, s * 0.74f, s * 0.75f, s * 0.8f, s * 0.5f, s * 0.8f)
                cubicTo(s * 0.25f, s * 0.8f, s * 0.08f, s * 0.74f, s * 0.04f, s * 0.6f)
                close()
            }
            drawPath(brim, color)
        }
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Gold,
    content: Color = Palette.Ink,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = content,
            disabledContainerColor = Color.White.copy(alpha = 0.12f),
            disabledContentColor = Color.White.copy(alpha = 0.4f),
        ),
        modifier = modifier.heightIn(min = 54.dp),
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = Pill, color = color.copy(alpha = 0.22f), modifier = modifier) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
