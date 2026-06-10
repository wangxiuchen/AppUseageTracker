package com.appspy.ui.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.appspy.ui.theme.Divider
import com.appspy.ui.theme.InkSub

/**
 * 纯 Canvas 折线图。
 *
 * @param series     每条线的数据；颜色 + float 值列表（长度应与 xLabels 一致）
 * @param xLabels    X 轴标签（日期简称）
 * @param metric     当前指标（时长或次数），影响 Y 轴格式
 */
@Composable
fun LineChart(
    series: List<Pair<Color, List<Float>>>,
    xLabels: List<String>,
    metric: TrendMetric,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty() || xLabels.isEmpty()) return

    val allValues = series.flatMap { it.second }
    val maxVal = allValues.maxOrNull()?.takeIf { it > 0f } ?: 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val topPad = 24f
        val bottomPad = 28f
        val leftPad = 42f
        val rightPad = 12f
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad
        val n = xLabels.size

        val gridLines = 3
        val gridPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 0xEC, 0xED, 0xF2)
            strokeWidth = 1f
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 0xA9, 0xAA, 0xB6)
            textSize = 22f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
        val xLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 0xA9, 0xAA, 0xB6)
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // ── 水平网格线 + Y 轴标签 ─────────────────────────────
        for (i in 0..gridLines) {
            val y = topPad + chartH * (1f - i.toFloat() / gridLines)
            drawContext.canvas.nativeCanvas.drawLine(leftPad, y, size.width - rightPad, y, gridPaint)

            val labelVal = maxVal * i / gridLines
            val labelStr = when (metric) {
                TrendMetric.DURATION -> formatYDuration(labelVal)
                TrendMetric.COUNT    -> labelVal.toInt().toString()
            }
            drawContext.canvas.nativeCanvas.drawText(
                labelStr, leftPad - 6f, y + 8f, labelPaint
            )
        }

        // ── X 轴标签 ──────────────────────────────────────────
        val xStep = if (n > 1) chartW / (n - 1) else chartW
        xLabels.forEachIndexed { idx, label ->
            val x = leftPad + idx * xStep
            drawContext.canvas.nativeCanvas.drawText(
                label, x, size.height - 4f, xLabelPaint
            )
        }

        // ── 折线 ──────────────────────────────────────────────
        series.forEach { (color, values) ->
            if (values.isEmpty()) return@forEach
            val points = values.mapIndexed { idx, v ->
                val x = leftPad + idx * xStep
                val y = topPad + chartH * (1f - v / maxVal)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    // 用简单直线（贝塞尔平滑可后续加）
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path, color,
                style = Stroke(
                    width = 2.2f * density,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 末端圆点
            drawCircle(color, radius = 3.5f * density, center = points.last())
        }
    }
}

/** 时长值 → "3h" / "45m" 等短标签 */
private fun formatYDuration(ms: Float): String {
    val totalSec = (ms / 1000).toLong()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return when {
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        else  -> "${totalSec}s"
    }
}
