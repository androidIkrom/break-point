package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.BackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF1E293B)
private val InkMuted = Color(0xFF64748B)
private val Grid = Color(0xFFE2E8F0)
private val Track = Color(0xFFF1F5F9)
// Single series: one hue for every bar; status colors are only used next to icons and labels
private val BarColor = Color(0xFF6366F1)

private fun levelColor(level: PerformanceLevel): Color = when (level) {
    PerformanceLevel.Excellent -> Color(0xFF059669)
    PerformanceLevel.Good -> Color(0xFFB45309)
    PerformanceLevel.Low -> Color(0xFFDC2626)
    PerformanceLevel.NoData -> Color(0xFF94A3B8)
}

private fun levelIcon(level: PerformanceLevel): ImageVector = when (level) {
    PerformanceLevel.Excellent -> Icons.Default.CheckCircle
    PerformanceLevel.Good -> Icons.Default.Info
    PerformanceLevel.Low -> Icons.Default.Warning
    PerformanceLevel.NoData -> Icons.Default.RemoveCircleOutline
}

private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

private fun pct(value: Int?): String = value?.let { "$it%" } ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
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

    Box(Modifier.fillMaxSize().background(AppDesign.BackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Monitoring markazi", fontWeight = FontWeight.Black, color = Color(0xFF065F46)) },
                    navigationIcon = { BackButton(onClick = onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (loaded == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BarColor)
                }
                return@Scaffold
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        Modifier.widthIn(max = 1200.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Ta'lim tahlili", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Ink)
                            Text(
                                if (byGroups) "Barcha guruhlar bo'yicha" else "${selectedGroup?.name} guruhi tinglovchilari",
                                color = InkMuted
                            )
                        }
                        GroupFilter(groups, selectedGroup, onSelect = { selectedGroup = it })
                    }
                }

                item { StatTiles(overall, Modifier.widthIn(max = 1200.dp).fillMaxWidth()) }

                item {
                    Section(
                        title = if (byGroups) "Guruhlar reytingi" else "Tinglovchilar reytingi",
                        subtitle = "O'rtacha natija, %",
                        modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                    ) {
                        RankingBars(rows, byGroups)
                    }
                }

                item {
                    Section(
                        title = "Batafsil jadval",
                        subtitle = if (byGroups) "Guruhni tanlab, tinglovchilar natijasini ko'ring" else null,
                        modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                    ) {
                        PerformanceTable(rows, byGroups, onGroupClick = { key ->
                            groups.firstOrNull { "group:${it.id}" == key }?.let { selectedGroup = it }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupFilter(groups: List<Group>, selected: Group?, onSelect: (Group?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = Ink)
            Spacer(Modifier.width(8.dp))
            Text(
                selected?.name ?: "Barcha guruhlar",
                color = Ink,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Barcha guruhlar", fontWeight = FontWeight.Bold) }, onClick = { onSelect(null); open = false })
            groups.forEach { g ->
                DropdownMenuItem(text = { Text(g.name) }, onClick = { onSelect(g); open = false })
            }
        }
    }
}

@Composable
private fun StatTiles(overall: PerformanceRow, modifier: Modifier) {
    val level = PerformanceLevel.of(overall.average)
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth < 700.dp) 2 else 4
        val tiles = listOf(
            Triple("O'rtacha natija", pct(overall.average), level),
            Triple("Urinishlar", overall.attempts.toString(), null),
            Triple("Tinglovchilar", overall.students.toString(), null),
            Triple("Eng yaxshi natija", pct(overall.best), null)
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            tiles.chunked(columns).forEach { rowTiles ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowTiles.forEach { (label, value, tileLevel) ->
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = AppDesign.ComponentShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Grid),
                            shadowElevation = 2.dp
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(label, color = InkMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(value, color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                                if (tileLevel != null) StatusChip(tileLevel)
                            }
                        }
                    }
                    repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String?, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = AppDesign.CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, Grid),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Ink)
            if (subtitle != null) Text(subtitle, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/** Horizontal bars: names stay readable however long they are. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankingBars(rows: List<PerformanceRow>, byGroups: Boolean) {
    val withData = rows.filter { it.average != null }
    if (withData.isEmpty()) {
        EmptyNote("Hali natijalar yo'q")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        withData.forEachIndexed { index, row ->
            TooltipArea(
                tooltip = {
                    Surface(shape = RoundedCornerShape(10.dp), color = Ink, shadowElevation = 6.dp) {
                        Column(Modifier.padding(12.dp)) {
                            Text(row.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("O'rtacha: ${pct(row.average)}", color = Color.White)
                            Text("Eng yaxshi: ${pct(row.best)} • Eng past: ${pct(row.worst)}", color = Color.White)
                            Text(
                                if (byGroups) "Tinglovchilar: ${row.students} • Urinishlar: ${row.attempts}" else "Urinishlar: ${row.attempts}",
                                color = Color.White
                            )
                        }
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(12.dp, 12.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = InkMuted, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                    Text(
                        row.name,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(220.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BoxWithConstraints(Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(4.dp)).background(Track)) {
                        val fraction = (row.average ?: 0) / 100f
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(maxWidth * fraction.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .background(BarColor)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(pct(row.average), color = Ink, fontWeight = FontWeight.Black, textAlign = TextAlign.End, modifier = Modifier.width(56.dp))
                }
            }
        }
        // Scale reference under the bars
        Row {
            Spacer(Modifier.width(28.dp + 220.dp + 12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0%", "25%", "50%", "75%", "100%").forEach {
                    Text(it, color = InkMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(12.dp + 56.dp))
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
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Track).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            columns.forEach { c ->
                Text(
                    c.title,
                    modifier = Modifier.weight(c.weight).padding(horizontal = 4.dp),
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = c.align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = Track)
            val level = PerformanceLevel.of(row.average)
            val cells: List<String> = if (byGroups) {
                listOf(row.name, row.students.toString(), row.attempts.toString(), pct(row.average), pct(row.best), pct(row.worst))
            } else {
                listOf(
                    row.name, row.attempts.toString(), pct(row.average), pct(row.best), pct(row.last),
                    row.lastTimestamp?.let { dateFormat.format(Date(it)) } ?: "—"
                )
            }
            val rowModifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).let {
                if (byGroups) it.then(Modifier.clickableRow { onGroupClick(row.key) }) else it
            }
            Row(rowModifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                cells.forEachIndexed { i, text ->
                    Text(
                        text,
                        modifier = Modifier.weight(columns[i].weight).padding(horizontal = 4.dp),
                        color = Ink,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                        textAlign = columns[i].align,
                        maxLines = if (i == 0) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(Modifier.weight(columns.last().weight).padding(horizontal = 4.dp)) { StatusChip(level) }
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@Composable
private fun StatusChip(level: PerformanceLevel) {
    val color = levelColor(level)
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(levelIcon(level), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        // Label text stays in ink, the icon carries the color
        Text(level.label, color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
        Text(text, color = InkMuted)
    }
}
