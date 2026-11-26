package com.pushlock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.Bg
import com.pushlock.app.ui.theme.Danger
import com.pushlock.app.ui.theme.Line
import com.pushlock.app.ui.theme.Surface1
import com.pushlock.app.ui.theme.Surface2
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.TextPrimary

enum class BtnStyle { Primary, Secondary, Ghost, Danger }

@Composable
fun PLButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val (bg, fg, border) = when (style) {
        BtnStyle.Primary -> Triple<Color, Color, Color?>(Accent, Bg, null)
        BtnStyle.Secondary -> Triple<Color, Color, Color?>(Surface2, TextPrimary, Line)
        BtnStyle.Ghost -> Triple<Color, Color, Color?>(Color.Transparent, TextMuted, null)
        BtnStyle.Danger -> Triple<Color, Color, Color?>(Color.Transparent, Danger, Danger.copy(alpha = 0.4f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.4f))
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = fg, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) Icon(icon, null, tint = if (enabled) fg else fg.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Text(
                    text,
                    color = if (enabled) fg else fg.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
fun PLCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Surface1)
            .border(1.dp, Line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Accent) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = Accent, boxSize: Dp = 44.dp) {
    Box(
        modifier
            .size(boxSize)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(boxSize * 0.5f))
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = TextPrimary) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface2)
            .border(1.dp, Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextMuted,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(start = 4.dp, top = 8.dp),
    )
}

/** Animated circular progress with room for content in the middle. */
@Composable
fun Ring(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    track: Color = Line,
    color: Color = Accent,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(450), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val topLeft = Offset(s / 2, s / 2)
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, topLeft = topLeft, size = arc, style = Stroke(s))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, false, topLeft = topLeft, size = arc, style = Stroke(s, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

@Composable
fun AppIconImage(bitmap: ImageBitmap?, boxSize: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(boxSize * 0.24f)
    if (bitmap != null) {
        Image(bitmap, null, modifier.size(boxSize).clip(shape))
    } else {
        Box(modifier.size(boxSize).clip(shape).background(Surface2))
    }
}

/** The PushLock mark: a padlock with an up-arrow cut out of it. Matches the launcher icon. */
@Composable
fun LogoMark(boxSize: Dp, modifier: Modifier = Modifier, tile: Boolean = true) {
    Canvas(modifier.size(boxSize)) {
        val u = this.size.minDimension / 108f
        if (tile) {
            drawRoundRect(Color(0xFF151A22), cornerRadius = CornerRadius(28f * u, 28f * u))
            drawRoundRect(Line, cornerRadius = CornerRadius(28f * u, 28f * u), style = Stroke(1.5f * u))
        }
        val shackle = Path().apply {
            moveTo(42f * u, 50f * u)
            lineTo(42f * u, 40f * u)
            arcTo(Rect(42f * u, 28f * u, 66f * u, 52f * u), 180f, 180f, false)
            lineTo(66f * u, 50f * u)
        }
        drawPath(shackle, Accent, style = Stroke(6f * u, cap = StrokeCap.Round))

        val body = Path().apply { addRoundRect(RoundRect(34f * u, 50f * u, 74f * u, 84f * u, 7f * u, 7f * u)) }
        val arrow = Path().apply {
            moveTo(54f * u, 58f * u)
            lineTo(62f * u, 67f * u)
            lineTo(57f * u, 67f * u)
            lineTo(57f * u, 77f * u)
            lineTo(51f * u, 77f * u)
            lineTo(51f * u, 67f * u)
            lineTo(46f * u, 67f * u)
            close()
        }
        drawPath(Path.combine(PathOperation.Difference, body, arrow), Accent)
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedContainerColor = Surface1,
    unfocusedContainerColor = Surface1,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextMuted,
    cursorColor = Accent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLeadingIconColor = Accent,
    unfocusedLeadingIconColor = TextMuted,
    focusedTrailingIconColor = TextMuted,
    unfocusedTrailingIconColor = TextMuted,
)

@Composable
fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Bg,
    checkedTrackColor = Accent,
    checkedBorderColor = Accent,
    uncheckedThumbColor = TextMuted,
    uncheckedTrackColor = Surface2,
    uncheckedBorderColor = Line,
)
