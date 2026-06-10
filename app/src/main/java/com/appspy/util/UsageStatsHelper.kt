package com.appspy.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 从 UsageStatsManager 解析出结构化的 app 使用数据 */
object UsageStatsHelper {

    private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ─────────────────────────────────────────────────────────
    // 核心模型
    // ─────────────────────────────────────────────────────────

    data class SessionData(
        val startTime: Long,
        val endTime: Long,
        val durationMs: Long
    )

    data class AppUsageData(
        val packageName: String,
        val appLabel: String,
        /** ACTIVITY_RESUMED 事件数，来回切算多次 */
        val openCount: Int,
        /** 当天/该时段前台总时长（毫秒） */
        val totalForegroundMs: Long,
        val sessions: List<SessionData>
    )

    // 内部用于存储原始事件，避免 UsageEvents.Event 对象被复用的问题
    private data class RawEvent(
        val packageName: String,
        val eventType: Int,
        val timeStamp: Long
    )

    // ─────────────────────────────────────────────────────────
    // 主入口：查询任意时间段
    // ─────────────────────────────────────────────────────────

    /**
     * 查询 [startTime, endTime) 区间内每个 app 的使用情况。
     *
     * @param filterSelf 是否过滤掉本 app 自己（默认 true）
     */
    fun queryUsageForRange(
        context: Context,
        startTime: Long,
        endTime: Long,
        filterSelf: Boolean = true
    ): List<AppUsageData> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(startTime, endTime) ?: return emptyList()

        // 采集原始事件（Event 对象会被复用，必须立即解包）
        val rawEvents = mutableListOf<RawEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            ) {
                rawEvents.add(RawEvent(event.packageName, event.eventType, event.timeStamp))
            }
        }

        // 按包名分组
        val byPackage = rawEvents.groupBy { it.packageName }

        return byPackage.mapNotNull { (pkg, pkgEvents) ->
            if (filterSelf && pkg == context.packageName) return@mapNotNull null

            val sorted = pkgEvents.sortedBy { it.timeStamp }
            var openCount = 0
            var totalMs = 0L
            val sessions = mutableListOf<SessionData>()
            var lastResumeTime: Long? = null

            for (ev in sorted) {
                when (ev.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        openCount++
                        lastResumeTime = ev.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        lastResumeTime?.let { resumeTime ->
                            val dur = ev.timeStamp - resumeTime
                            if (dur > 0) {
                                totalMs += dur
                                sessions.add(SessionData(resumeTime, ev.timeStamp, dur))
                            }
                            lastResumeTime = null
                        }
                    }
                }
            }

            // 仍在前台（只有 RESUMED 没有对应 PAUSED）
            lastResumeTime?.let { resumeTime ->
                val dur = endTime - resumeTime
                if (dur > 0) {
                    totalMs += dur
                    sessions.add(SessionData(resumeTime, endTime, dur))
                }
            }

            if (openCount == 0) return@mapNotNull null

            AppUsageData(
                packageName = pkg,
                appLabel = getAppLabel(context, pkg),
                openCount = openCount,
                totalForegroundMs = totalMs,
                sessions = sessions
            )
        }.sortedByDescending { it.totalForegroundMs }
    }

    // ─────────────────────────────────────────────────────────
    // 便捷方法：今日（当天 00:00 到现在）
    // ─────────────────────────────────────────────────────────

    fun queryToday(context: Context): List<AppUsageData> {
        val now = System.currentTimeMillis()
        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return queryUsageForRange(context, todayStart, now)
    }

    // ─────────────────────────────────────────────────────────
    // 按自然日分组：适合落库时补抓历史
    // ─────────────────────────────────────────────────────────

    /**
     * 查询某日（yyyy-MM-dd）整天（00:00 ~ 23:59:59.999）的使用情况。
     */
    fun queryDate(context: Context, date: LocalDate): List<AppUsageData> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return queryUsageForRange(context, start, end)
    }

    // ─────────────────────────────────────────────────────────
    // 辅助
    // ─────────────────────────────────────────────────────────

    fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName  // 已卸载 app 兜底显示包名
        }
    }

    fun dateToString(date: LocalDate): String = date.format(DATE_FMT)
    fun stringToDate(s: String): LocalDate = LocalDate.parse(s, DATE_FMT)
}
