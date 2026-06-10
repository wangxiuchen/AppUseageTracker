package com.appspy.ui.trend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appspy.data.db.entity.AppMeta
import com.appspy.ui.theme.*
import com.appspy.util.TimeFormatter

// 多条折线轮换使用的颜色
private val SERIES_COLORS = listOf(Primary, Coral, Color(0xFF16A06A), Color(0xFFF59E0B))

@Composable
fun TrendScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: TrendViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.selectorOpen) {
        AppSelectorSheet(
            apps = state.availableApps,
            selected = state.selectedPackages,
            onToggle = { viewModel.toggleApp(it) },
            onDismiss = { viewModel.closeSelector() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 19.dp, vertical = 0.dp)
    ) {
        // ── 顶栏 ────────────────────────────────────────────────
        Text(
            "趋势",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.padding(top = 16.dp, bottom = 14.dp)
        )

        // ── App 选择 chip 行 ────────────────────────────────────
        AppChipsRow(
            selected = state.selectedPackages,
            availableApps = state.availableApps,
            seriesColors = SERIES_COLORS,
            onAddClick = { viewModel.openSelector() },
            onRemove = { viewModel.toggleApp(it) }
        )

        Spacer(Modifier.height(12.dp))

        // ── 时间范围 + 指标切换 ─────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DualSegment(
                options = TrendRange.entries.map { it.label },
                selectedIndex = TrendRange.entries.indexOf(state.range),
                onSelect = { viewModel.setRange(TrendRange.entries[it]) }
            )
            DualSegment(
                options = listOf("时长", "次数"),
                selectedIndex = if (state.metric == TrendMetric.DURATION) 0 else 1,
                onSelect = { viewModel.setMetric(if (it == 0) TrendMetric.DURATION else TrendMetric.COUNT) }
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── 图例 ────────────────────────────────────────────────
        if (state.series.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                state.series.forEachIndexed { idx, s ->
                    LegendItem(label = s.appLabel, color = SERIES_COLORS.getOrElse(idx) { Primary })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── 折线图 ──────────────────────────────────────────────
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }
            state.selectedPackages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Chip),
                    contentAlignment = Alignment.Center
                ) {
                    Text("点击「+ 添加应用」开始查看趋势", color = InkSub, fontSize = 13.sp)
                }
            }
            state.series.all { s -> s.values.all { it == 0f } } -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Chip),
                    contentAlignment = Alignment.Center
                ) {
                    Text("该时间段内暂无数据", color = InkSub, fontSize = 13.sp)
                }
            }
            else -> {
                LineChart(
                    series = state.series.mapIndexed { idx, s ->
                        SERIES_COLORS.getOrElse(idx) { Primary } to s.values
                    },
                    xLabels = state.xLabels,
                    metric = state.metric
                )
            }
        }

        // ── 汇总卡片 ────────────────────────────────────────────
        if (state.series.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.series.forEachIndexed { idx, s ->
                    val color = SERIES_COLORS.getOrElse(idx) { Primary }
                    TrendSummaryCard(
                        appLabel = s.appLabel,
                        color = color,
                        totalMs = s.totalMs,
                        totalCount = s.totalCount,
                        rangeDays = state.range.days,
                        metric = state.metric,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── App chip 行 ───────────────────────────────────────────────

@Composable
private fun AppChipsRow(
    selected: Set<String>,
    availableApps: List<AppMeta>,
    seriesColors: List<Color>,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit
) {
    val selectedList = selected.toList()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        selectedList.forEachIndexed { idx, pkg ->
            val label = availableApps.find { it.packageName == pkg }?.appLabel ?: pkg
            val color = seriesColors.getOrElse(idx) { Primary }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .clickable { onRemove(pkg) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        // + 添加按钮
        if (selectedList.size < 4) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFC7C9F2), RoundedCornerShape(20.dp))
                    .clickable { onAddClick() }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text("+ 添加应用", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 双选 Segment ──────────────────────────────────────────────

@Composable
private fun DualSegment(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Chip)
            .padding(3.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 13.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Ink else InkSub
                )
            }
        }
    }
}

// ── 图例 ──────────────────────────────────────────────────────

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(9.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.5.sp, color = InkSub)
    }
}

// ── 汇总卡片 ──────────────────────────────────────────────────

@Composable
private fun TrendSummaryCard(
    appLabel: String,
    color: Color,
    totalMs: Long,
    totalCount: Int,
    rangeDays: Int,
    metric: TrendMetric,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // 小圆点 + App 名
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color) }
            Spacer(Modifier.width(6.dp))
            Text(appLabel, fontSize = 11.sp, color = InkSub,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        when (metric) {
            TrendMetric.DURATION -> {
                Text(TimeFormatter.formatDuration(totalMs), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("日均 ${TimeFormatter.formatDuration(if (rangeDays > 0) totalMs / rangeDays else 0)}",
                    fontSize = 10.5.sp, color = InkSub, modifier = Modifier.padding(top = 2.dp))
            }
            TrendMetric.COUNT -> {
                Text("$totalCount 次", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                val avg = if (rangeDays > 0) totalCount / rangeDays else 0
                Text("日均 $avg 次", fontSize = 10.5.sp, color = InkSub, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
