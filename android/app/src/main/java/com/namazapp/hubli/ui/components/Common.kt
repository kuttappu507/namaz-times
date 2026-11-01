package com.namazapp.hubli.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.namazapp.hubli.domain.Prayer
import com.namazapp.hubli.ui.theme.AppTheme

/** Outlined icon that suits each prayer's time of day. */
fun Prayer.icon(): ImageVector = when (this) {
    Prayer.FAJR -> Icons.Outlined.WbTwilight
    Prayer.SUNRISE -> Icons.Outlined.LightMode
    Prayer.ZUHR -> Icons.Outlined.WbSunny
    Prayer.ASR -> Icons.Outlined.Brightness6
    Prayer.MAGHRIB -> Icons.Outlined.DarkMode
    Prayer.ISHA -> Icons.Outlined.NightsStay
}

/** Per-prayer accent gradient used on badges. */
fun Prayer.gradient(): List<Color> = when (this) {
    Prayer.FAJR -> listOf(Color(0xFF5B6CD6), Color(0xFF9A7BD9))
    Prayer.SUNRISE -> listOf(Color(0xFFF7A54B), Color(0xFFFFD36B))
    Prayer.ZUHR -> listOf(Color(0xFF16A37A), Color(0xFF5CD6AA))
    Prayer.ASR -> listOf(Color(0xFFD9A62F), Color(0xFFF2CB6A))
    Prayer.MAGHRIB -> listOf(Color(0xFFE5643F), Color(0xFFF59A6B))
    Prayer.ISHA -> listOf(Color(0xFF2B3A8C), Color(0xFF5B6CD6))
}

@Composable
fun PrayerIconBadge(
    prayer: Prayer,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = true,
) {
    val colors = if (active) prayer.gradient() else listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val tint by animateColorAsState(
        if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(300), label = "badgeTint",
    )
    Box(
        modifier = modifier
            .size(size)
            .then(if (active) Modifier.shadow(6.dp, CircleShape, spotColor = colors.first().copy(alpha = 0.5f)) else Modifier)
            .background(Brush.linearGradient(colors), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = prayer.icon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Small rounded label such as "NOW" or "NEXT". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    outlined: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (outlined) Modifier.border(1.dp, content.copy(alpha = 0.6f), CircleShape)
                else Modifier.background(container),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

/**
 * The app's standard elevated surface: soft coloured shadow, large radius, subtle vertical
 * gradient and a hairline highlight border so cards read clearly on both light and dark
 * backgrounds.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    contentPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val ext = AppTheme.extended
    val borderColor = if (ext.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.9f)
    Box(
        modifier
            .shadow(
                elevation = if (ext.isDark) 10.dp else 16.dp,
                shape = shape,
                ambientColor = if (ext.isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0B7A5B).copy(alpha = 0.14f),
                spotColor = if (ext.isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0B7A5B).copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(ext.cardGradientStart, ext.cardGradientEnd)))
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
    ) { content() }
}

/** Section heading with a small gold rule. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .size(width = 28.dp, height = 3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(AppTheme.extended.gold, AppTheme.extended.gold.copy(alpha = 0f))),
                        CircleShape,
                    ),
            )
        }
        trailing?.invoke()
    }
}
