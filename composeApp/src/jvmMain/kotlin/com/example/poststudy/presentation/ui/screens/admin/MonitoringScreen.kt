package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun levelColors(level: PerformanceLevel): Triple<Color, Color, Color> = when (level) {
    PerformanceLevel.Excellent -> Triple(Signal.SuccessContainer, Signal.OnSuccessContainer, Signal.Success)
    PerformanceLevel.Good -> Triple(Signal.WarningContainer, Signal.OnWarningContainer, Signal.Warning)
    PerformanceLevel.Low -> Triple(Signal.DangerContainer, Signal.OnDangerContainer, Signal.Danger)
    PerformanceLevel.NoData -> Triple(Signal.SurfaceContainer, Signal.Muted, Signal.Faint)
}

private fun levelIcon(level: PerformanceLevel): ImageVector = when (level) {
    PerformanceLevel.Excellent -> Icons.Outlined.CheckCircle
    PerformanceLevel.Good -> Icons.Outlined.Info
    PerformanceLevel.Low -> Icons.Outlined.WarningAmber
    PerformanceLevel.NoData -> Icons.Outlined.RemoveCircleOutline
}

private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

private fun pct(value: Int?): String = value?.let { "$it%" } ?: "—"

@Composable
fun MonitoringScreen(subjectId: Int, onBack: () -> Unit) {
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var records by remember { mutableStateOf<List<ExamRecord>?>(null) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }

    LaunchedEffect(subjectId) {
        val (g, r) = withContext(Dispatchers.IO) {
            val repo = AppContainer.localRepository
            repo.getAllGroups(subjectId).first() to repo.getAllExamRecords(subjectId).first()
        }
        groups = g
        records = r
    }

    val loaded = records
    val scoped = loaded.orEmpty().filter { selectedGroup == null || it.groupId == selectedGroup?.id }
    val rows = remember(loaded, selectedGroup, groups) {
        if (selectedGroup == null) Statistics.byGroup(loaded.orEmpty(), groups) else Statistics.byStudent(scoped)
    }
    val overall = remember(loaded, selectedGroup) { Statistics.overall(scoped) }
    val byGroups = selectedGroup == null

    AdminPage(
        title = "Monitoring markazi",
        eyebrow = "Ta'lim tahlili",
        subtitle = if (byGroups) "Barcha guruhlar bo'yicha" else "${selectedGroup?.name} guruhi tinglovchilari",
        actions = { GroupFilter(groups, selectedGroup, onSelect = { selectedGroup = it }) }
    ) {
        if (loaded == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
            }
            return@AdminPage
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.xl),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { StatTiles(overall, Modifier.fillMaxWidth()) }

            item {
                SignalCard(padding = 22.dp) {
                    SectionTitle(
                        title = if (byGroups) "Guruhlar reytingi" else "Tinglovchilar reytingi",
                        subtitle = "O'rtacha natija, %. Ustiga olib boring: eng yaxshi, eng past, urinishlar."
                    )
                    Spacer(Modifier.height(16.dp))
                    RankingBars(rows, byGroups)
                }
            }

            item {
                SignalCard(padding = 0.dp) {
                    Box(Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 10.dp)) {
                        SectionTitle(
                            title = "Batafsil jadval",
                            subtitle = if (byGroups) "Guruhni tanlab, tinglovchilar natijasini ko'ring" else null
                        )
                    }
                    PerformanceTable(rows, byGroups, onGroupClick = { key ->
                        groups.firstOrNull { "group:${it.id}" == key }?.let { selectedGroup = it }
                    })
                }
            }
        }
    }
}

@Composable
private fun GroupFilter(groups: List<Group>, selected: Group?, onSelect: (Group?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SignalButton(selected?.name ?: "Barcha guruhlar", onClick = { open = true }, style = ButtonStyle.Outlined, trailingIcon = Icons.Default.KeyboardArrowDown, height = 42.dp)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = AppDesign.ComponentShape) {
            DropdownMenuItem(text = { Text("Barcha guruhlar") }, onClick = { onSelect(null); open = false })
            groups.forEach { g ->
                DropdownMenuItem(text = { Text(g.name) }, onClick = { onSelect(g); open = false })
            }
        }
    }
}

@Composable
private fun StatTiles(overall: PerformanceRow, modifier: Modifier) {
    val level = PerformanceLevel.of(overall.average)
    val (lvBg, lvFg, lvIcon) = levelColors(level)
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth < 700.dp) 2 else 4
        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { m ->
                Surface(modifier = m, shape = AppDesign.CardShape, color = Color.Transparent) {
                    Column(Modifier.background(Signal.GradientHeroTeal).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("O'rtacha natija", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
                        Text(pct(overall.average), style = MaterialTheme.typography.displaySmall, color = Signal.OnDark)
                        StatusChip(level.label, lvBg, lvFg, icon = levelIcon(level), iconTint = lvIcon)
                    }
                }
            },
            { m -> KpiTile("Urinishlar", overall.attempts.toString(), m, note = "Jami topshirilgan testlar") },
            { m -> KpiTile("Tinglovchilar", overall.students.toString(), m, note = "Natijasi bor tinglovchilar") },
            { m -> KpiTile("Eng yaxshi natija", pct(overall.best), m, note = "Eng past: ${pct(overall.worst)}", container = Signal.WarningContainer, contentColor = Signal.OnWarningContainer) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tiles.chunked(columns).forEach { rowTiles ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowTiles.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Horizontal bars that grow in; names stay readable however long they are. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankingBars(rows: List<PerformanceRow>, byGroups: Boolean) {
    val withData = rows.filter { it.average != null }
    if (withData.isEmpty()) {
        EmptyNote("Hali natijalar yo'q")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        withData.forEachIndexed { index, row ->
            TooltipArea(
                tooltip = {
                    Surface(shape = AppDesign.SmallShape, color = Signal.Dark, shadowElevation = 8.dp) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.name, color = Signal.OnDark, style = MaterialTheme.typography.titleSmall)
                            Text("O'rtacha: ${pct(row.average)}", color = Signal.OnDarkMuted, style = MaterialTheme.typography.bodySmall)
                            Text("Eng yaxshi: ${pct(row.best)} • Eng past: ${pct(row.worst)}", color = Signal.OnDarkMuted, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (byGroups) "Tinglovchilar: ${row.students} • Urinishlar: ${row.attempts}" else "Urinishlar: ${row.attempts}",
                                color = Signal.OnDarkMuted, style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(12.dp, 12.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).background(if (index == 0) Signal.Amber else Signal.SurfaceContainer, AppDesign.PillShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = if (index == 0) Signal.Dark else Signal.Muted)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(row.name, color = Signal.Ink, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(180.dp))
                    Spacer(Modifier.width(12.dp))
                    val fraction = (row.average ?: 0) / 100f
                    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), AppMotion.tweenOrSnap(900, AppMotion.decelerateEasing), label = "bar")
                    Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(0.dp, 6.dp, 6.dp, 0.dp)).background(Signal.SurfaceContainer)) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animated)
                                .clip(RoundedCornerShape(0.dp, 6.dp, 6.dp, 0.dp))
                                .background(if (index == 0) Brush.horizontalGradient(listOf(Signal.Primary, Signal.Cyan)) else Brush.horizontalGradient(listOf(Signal.Primary, Signal.Primary)))
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(pct(row.average), color = Signal.Ink, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
                }
            }
        }
        // Scale reference under the bars
        Row {
            Spacer(Modifier.width(24.dp + 10.dp + 180.dp + 12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0%", "25%", "50%", "75%", "100%").forEach {
                    Text(it, color = Signal.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(12.dp + 52.dp))
        }
    }
}

private data class TableColumn(val title: String, val weight: Float, val align: TextAlign = TextAlign.Start)

@Composable
private fun PerformanceTable(rows: List<PerformanceRow>, byGroups: Boolean, onGroupClick: (String) -> Unit) {
    if (rows.isEmpty()) {
        EmptyNote(if (byGroups) "Bu fanda guruhlar yo'q" else "Bu guruhda hali natijalar yo'q")
        return
    }
    val columns = if (byGroups) {
        listOf(
            TableColumn("Guruh", 2.2f), TableColumn("Tinglovchilar", 1.2f, TextAlign.End), TableColumn("Urinishlar", 1.1f, TextAlign.End),
            TableColumn("O'rtacha", 1f, TextAlign.End), TableColumn("Eng yaxshi", 1.1f, TextAlign.End),
            TableColumn("Eng past", 1f, TextAlign.End), TableColumn("Holat", 1.4f)
        )
    } else {
        listOf(
            TableColumn("Tinglovchi", 2.4f), TableColumn("Urinishlar", 1.1f, TextAlign.End), TableColumn("O'rtacha", 1f, TextAlign.End),
            TableColumn("Eng yaxshi", 1.1f, TextAlign.End), TableColumn("Oxirgi", 1f, TextAlign.End),
            TableColumn("Oxirgi sana", 1.6f, TextAlign.End), TableColumn("Holat", 1.4f)
        )
    }

    Column {
        Row(Modifier.fillMaxWidth().background(Signal.SurfaceContainer).padding(horizontal = 22.dp, vertical = 10.dp)) {
            columns.forEach { c ->
                Text(c.title.uppercase(), modifier = Modifier.weight(c.weight).padding(horizontal = 4.dp), color = Signal.Muted, style = MaterialTheme.typography.labelSmall, textAlign = c.align, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = Signal.SurfaceContainer)
            val level = PerformanceLevel.of(row.average)
            val (lvBg, lvFg, lvIcon) = levelColors(level)
            val cells: List<String> = if (byGroups) {
                listOf(row.name, row.students.toString(), row.attempts.toString(), pct(row.average), pct(row.best), pct(row.worst))
            } else {
                listOf(
                    row.name, row.attempts.toString(), pct(row.average), pct(row.best), pct(row.last),
                    row.lastTimestamp?.let { dateFormat.format(Date(it)) } ?: "—"
                )
            }
            val rowModifier = Modifier.fillMaxWidth().let { if (byGroups) it.clickable { onGroupClick(row.key) } else it }
            Row(rowModifier.padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                cells.forEachIndexed { i, text ->
                    Text(
                        text,
                        modifier = Modifier.weight(columns[i].weight).padding(horizontal = 4.dp),
                        color = Signal.Ink,
                        style = if (i == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        textAlign = columns[i].align,
                        maxLines = if (i == 0) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(Modifier.weight(columns.last().weight).padding(horizontal = 4.dp)) {
                    StatusChip(level.label, lvBg, lvFg, icon = levelIcon(level), iconTint = lvIcon)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Signal.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
