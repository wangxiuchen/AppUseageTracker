package com.appspy.data.repository

import android.content.Context
import com.appspy.data.db.dao.AppMetaDao
import com.appspy.data.db.dao.DailyUsageDao
import com.appspy.data.db.dao.SessionRecordDao
import com.appspy.data.db.entity.AppMeta
import com.appspy.data.db.entity.DailyUsage
import com.appspy.data.db.entity.SessionRecord
import com.appspy.util.UsageStatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appMetaDao: AppMetaDao,
    private val dailyUsageDao: DailyUsageDao,
    private val sessionRecordDao: SessionRecordDao
) {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ─────────────────────────────────────────────────────────
    // 实时查询（今日页）
    // ─────────────────────────────────────────────────────────

    /** 实时拉取今天的使用数据（直接读系统事件） */
    suspend fun getTodayUsageLive(): List<UsageStatsHelper.AppUsageData> =
        UsageStatsHelper.queryToday(context)

    // ─────────────────────────────────────────────────────────
    // 趋势数据（从 Room 读）
    // ─────────────────────────────────────────────────────────

    suspend fun getDailyUsageForApps(
        packages: List<String>,
        startDate: String,
        endDate: String
    ): List<DailyUsage> =
        dailyUsageDao.getForAppsInRange(packages, startDate, endDate)

    fun getDistinctPackagesFlow(): Flow<List<String>> =
        dailyUsageDao.getDistinctPackagesFlow()

    suspend fun getDistinctPackages(): List<String> =
        dailyUsageDao.getDistinctPackages()

    suspend fun getAppLabel(packageName: String): String {
        return appMetaDao.getByPackage(packageName)?.appLabel
            ?: UsageStatsHelper.getAppLabel(context, packageName)
    }

    suspend fun getAllAppMeta(): List<AppMeta> = appMetaDao.getAll()

    // ─────────────────────────────────────────────────────────
    // 落库（WorkManager / 打开 App 时触发）
    // ─────────────────────────────────────────────────────────

    /**
     * 把指定日期的使用数据写入 Room。
     * 同一天可被多次调用（REPLACE），保证数据最新。
     */
    suspend fun snapshotDate(date: LocalDate) {
        val dateStr = date.format(dateFmt)
        val usageList = if (date == LocalDate.now()) {
            UsageStatsHelper.queryToday(context)
        } else {
            UsageStatsHelper.queryDate(context, date)
        }

        if (usageList.isEmpty()) return

        // 更新 AppMeta
        val metas = usageList.map { AppMeta(it.packageName, it.appLabel) }
        appMetaDao.upsertAll(metas)

        // 更新 DailyUsage
        val dailyRows = usageList.map { u ->
            DailyUsage(
                date = dateStr,
                packageName = u.packageName,
                openCount = u.openCount,
                totalForegroundMs = u.totalForegroundMs
            )
        }
        dailyUsageDao.upsertAll(dailyRows)

        // 更新 SessionRecord（先清该天旧数据，再插入）
        usageList.forEach { u ->
            sessionRecordDao.deleteForDateAndApp(dateStr, u.packageName)
            val records = u.sessions.map { s ->
                SessionRecord(
                    packageName = u.packageName,
                    date = dateStr,
                    startTime = s.startTime,
                    endTime = s.endTime,
                    durationMs = s.durationMs
                )
            }
            if (records.isNotEmpty()) sessionRecordDao.insertAll(records)
        }
    }

    /**
     * 检查最近 [lookbackDays] 天内哪些日期没有数据，
     * 如果系统事件还能查到就补抓（防止某天 app 未打开导致漏记）。
     */
    suspend fun fillGaps(lookbackDays: Int = 7) {
        val today = LocalDate.now()
        val sinceStr = today.minusDays(lookbackDays.toLong()).format(dateFmt)
        val existingDates = dailyUsageDao.getDatesWithDataSince(sinceStr).toSet()

        for (offset in 1..lookbackDays) {
            val date = today.minusDays(offset.toLong())
            val dateStr = date.format(dateFmt)
            if (dateStr !in existingDates) {
                snapshotDate(date)
            }
        }
    }
}
