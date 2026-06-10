package com.appspy.di

import android.content.Context
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appspy.data.db.AppDatabase
import com.appspy.data.db.dao.AppMetaDao
import com.appspy.data.db.dao.DailyUsageDao
import com.appspy.data.db.dao.SessionRecordDao
import com.appspy.worker.DailySnapshotWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "app_usage.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAppMetaDao(db: AppDatabase): AppMetaDao = db.appMetaDao()
    @Provides fun provideDailyUsageDao(db: AppDatabase): DailyUsageDao = db.dailyUsageDao()
    @Provides fun provideSessionRecordDao(db: AppDatabase): SessionRecordDao = db.sessionRecordDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext ctx: Context): WorkManager =
        WorkManager.getInstance(ctx)
}

/** 在 Application 或 MainActivity 中调用此函数，注册一次性周期任务 */
fun scheduleSnapshotWork(workManager: WorkManager) {
    val request = PeriodicWorkRequestBuilder<DailySnapshotWorker>(
        repeatInterval = 24,
        repeatIntervalTimeUnit = TimeUnit.HOURS
    ).build()

    workManager.enqueueUniquePeriodicWork(
        DailySnapshotWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,  // 已存在则不重置（保留现有调度）
        request
    )
}
