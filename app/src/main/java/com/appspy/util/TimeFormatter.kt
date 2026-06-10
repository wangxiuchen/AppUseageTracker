package com.appspy.util

/** 时长格式化工具：毫秒 → "1h 23m" / "12m" / "45s" */
object TimeFormatter {

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0s"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0           -> "${h}h"
            m > 0           -> "${m}m"
            else            -> "${s}s"
        }
    }

    /** 简短版：只显示最粗粒度，如 "1.3h" "23m" "45s" */
    fun formatShort(ms: Long): String {
        if (ms <= 0) return "0s"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "%.1fh".format(ms / 3_600_000.0)
            m > 0 -> "${m}m"
            else  -> "${s}s"
        }
    }
}
