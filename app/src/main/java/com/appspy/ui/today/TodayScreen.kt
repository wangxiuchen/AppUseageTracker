package com.appspy.ui.today

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appspy.ui.theme.*
import com.appspy.util.TimeFormatter
import com.appspy.util.UsageStatsHelper

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.loadTodayData() },
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 19.dp, vertical = 0.dp)
        ) {
            // ── 顶栏 ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("今日", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(state.dateLabel, fontSize = 12.sp, color = InkSub)
                }
            }

            // ── 汇总卡片（三格）────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCell(
                        value = TimeFormatter.formatDuration(state.totalDurationMs),
                        label = "总时长",
                        bgColor = PrimarySoft,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCell(
                        value = state.totalOpenCount.toString(),
                        label = "打开次数",
                        bgColor = CoralSoft,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCell(
                        value = state.activeApps.toString(),
                        label = "活跃应用",
                        bgColor = GreenSoft,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 排序切换 Segment ──────────────────────────────────
            item {
                SortSegment(
                    current = state.sortMode,
                    onSelect = { viewModel.setSortMode(it) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // ── App 列表 ──────────────────────────────────────────
            if (state.appList.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("今天还没有使用记录", color = InkSub, fontSize = 14.sp)
                    }
                }
            } else {
                val maxMs = state.appList.firstOrNull()?.let {
                    if (state.sortMode == SortMode.BY_DURATION) it.totalForegroundMs
                    else state.appList.maxOfOrNull { a -> a.totalForegroundMs } ?: 1L
                } ?: 1L
                val maxCount = state.appList.maxOfOrNull { it.openCount }?.toFloat() ?: 1f

                items(state.appList, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        barFraction = when (state.sortMode) {
                            SortMode.BY_DURATION -> if (maxMs > 0) app.totalForegroundMs.toFloat() / maxMs else 0f
                            SortMode.BY_COUNT    -> if (maxCount > 0) app.openCount.toFloat() / maxCount else 0f
                        },
                        barColor = Primary,
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = 14.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── 汇总格 ────────────────────────────────────────────────────

@Composable
private fun SummaryCell(value: String, label: String, bgColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(label, fontSize = 10.5.sp, color = InkSub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ── 排序 Segment ──────────────────────────────────────────────

@Composable
private fun SortSegment(current: SortMode, onSelect: (SortMode) -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Chip)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        SegmentTab("按时长", current == SortMode.BY_DURATION) { onSelect(SortMode.BY_DURATION) }
        SegmentTab("按次数", current == SortMode.BY_COUNT) { onSelect(SortMode.BY_COUNT) }
    }
}

@Composable
private fun SegmentTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Ink else InkSub
        )
    }
}

// ── App 行 ───────────────────────────────────────────────────

@Composable
private fun AppRow(
    app: UsageStatsHelper.AppUsageData,
    barFraction: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon: Drawable? = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon.toBitmap(40, 40).asImageBitmap(),
                    contentDescription = app.appLabel,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = app.appLabel.take(1).uppercase(),
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.width(11.dp))

        // 名称 + 进度条
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appLabel,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Divider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = barFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }

        Spacer(Modifier.width(11.dp))

        // 时长 + 次数
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TimeFormatter.formatDuration(app.totalForegroundMs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "${app.openCount} 次",
                fontSize = 10.5.sp,
                color = InkSub,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
