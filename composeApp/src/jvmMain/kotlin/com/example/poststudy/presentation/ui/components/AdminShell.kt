package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space

/** Sections of the admin area reachable from the rail. */
enum class AdminSection(val label: String, val icon: ImageVector) {
    Home("Bosh sahifa", Icons.Outlined.Home),
    Lessons("Darslar", Icons.AutoMirrored.Outlined.MenuBook),
    Exams("Imtihon", Icons.Outlined.Assignment),
    Groups("Guruhlar", Icons.Outlined.Groups),
    Monitoring("Monitoring", Icons.Outlined.BarChart),
    History("Tahlil", Icons.Outlined.History),
    Help("Yordam", Icons.AutoMirrored.Outlined.HelpOutline)
}

/** Set by the app root so any admin screen can jump between sections. */
val LocalAdminNav = staticCompositionLocalOf<(AdminSection) -> Unit> { {} }

/** A form screen sets [isDirty] while it holds unsaved input; the rail then asks before leaving. */
class UnsavedGuard {
    var isDirty: () -> Boolean = { false }
}

val LocalUnsavedGuard = staticCompositionLocalOf { UnsavedGuard() }

/**
 * Dark navigation rail on the left of every admin screen. The selected item carries a cyan
 * bar and tint; the indicator slides to the clicked item.
 */
@Composable
fun AdminRail(current: AdminSection?, onNavigate: (AdminSection) -> Unit, modifier: Modifier = Modifier) {
    val main = listOf(AdminSection.Home, AdminSection.Lessons, AdminSection.Exams, AdminSection.Groups, AdminSection.Monitoring, AdminSection.History)
    Column(
        modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(Signal.GradientRail)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InstituteLogo(size = 44.dp, glow = true)
        Spacer(Modifier.height(14.dp))
        Box {
            val index = main.indexOf(current).coerceAtLeast(0)
            val itemHeight = 64.dp
            val indicatorY by animateDpAsState(itemHeight * index, AppMotion.tweenOrSnap(AppMotion.normal), label = "railIndicator")
            if (current != null && current in main) {
                Box(
                    Modifier
                        .offset(y = indicatorY)
                        .width(80.dp)
                        .height(60.dp)
                        .background(Signal.Cyan.copy(alpha = 0.16f), AppDesign.ComponentShape)
                        .drawBehind { drawRect(Signal.Cyan, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                main.forEach { section -> RailItem(section, section == current, onNavigate) }
            }
        }
        Spacer(Modifier.weight(1f))
        RailItem(AdminSection.Help, current == AdminSection.Help, onNavigate)
    }
}

@Composable
private fun RailItem(section: AdminSection, selected: Boolean, onNavigate: (AdminSection) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint by animateColorAsState(
        when { selected -> Signal.Cyan; hovered -> Signal.OnDark; else -> Signal.OnDarkFaint },
        AppMotion.tweenOrSnap(AppMotion.fast), label = "railTint"
    )
    Surface(
        onClick = { onNavigate(section) },
        modifier = Modifier.width(80.dp).height(60.dp),
        shape = AppDesign.ComponentShape,
        color = if (hovered && !selected) Color.White.copy(alpha = 0.06f) else Color.Transparent,
        interactionSource = interaction
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(section.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(section.label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** White header bar for light admin screens: optional back button, eyebrow + title, actions on the right. */
@Composable
fun AdminHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxWidth(), color = Signal.Card, border = BorderStroke(1.dp, Signal.Outline)) {
        Row(
            Modifier.padding(horizontal = Space.page, vertical = 14.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (onBack != null) BackButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp))
            Column(Modifier.weight(1f)) {
                if (eyebrow != null) Eyebrow(eyebrow)
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Signal.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

/** Light admin page body: header on top, content centered up to the max content width. */
@Composable
fun AdminPage(
    title: String,
    eyebrow: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Signal.Surface)) {
        AdminHeader(title, eyebrow = eyebrow, subtitle = subtitle, onBack = onBack, actions = actions)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxSize(), content = content)
        }
    }
}
