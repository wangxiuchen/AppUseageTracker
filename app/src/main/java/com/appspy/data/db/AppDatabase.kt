package com.appspy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appspy.data.db.dao.AppMetaDao
import com.appspy.data.db.dao.DailyUsageDao
import com.appspy.data.db.dao.SessionRecordDao
import com.appspy.data.db.entity.AppMeta
import com.appspy.data.db.entity.DailyUsage
import com.appspy.data.db.entity.SessionRecord

@Database(
    entities = [AppMeta::class, DailyUsage::class, SessionRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun sessionRecordDao(): SessionRecordDao
}
