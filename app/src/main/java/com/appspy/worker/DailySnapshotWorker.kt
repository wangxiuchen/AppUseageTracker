package com.appspy.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appspy.data.repository.UsageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * 每日后台快照任务（WorkManager PeriodicWorkRequest，周期约 24h）。
 *
 * 职责：
 * 1. 对今天做一次快照（写/更新当天那行 DailyUsage）。
 * 2. 补抓最近 7 天内有缺口的日期（防止某天 app 未打开导致漏记）。
 */
@HiltWorker
class DailySnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: UsageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. 今天快照
            usageRepository.snapshotDate(LocalDate.now())
            // 2. 补抓缺口（最多往前 7 天）
            usageRepository.fillGaps(lookbackDays = 7)
            // 3. 清理历史上误入库的系统组件数据
            usageRepository.purgeNonLaunchablePackages()
            Result.success()
        } catch (e: Exception) {
            // 失败后最多重试 2 次
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "daily_snapshot"
    }
}
