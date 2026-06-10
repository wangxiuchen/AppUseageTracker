package com.appspy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appspy.data.db.entity.DailyUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyUsage: DailyUsage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<DailyUsage>)

    /** 查询某 app 在某日期区间内的所有记录，按日期升序 */
    @Query("""
        SELECT * FROM daily_usage
        WHERE packageName IN (:packages)
          AND date >= :startDate
          AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getForAppsInRange(
        packages: List<String>,
        startDate: String,
        endDate: String
    ): List<DailyUsage>

    /** 查询某一天有使用记录的所有 app */
    @Query("SELECT * FROM daily_usage WHERE date = :date ORDER BY totalForegroundMs DESC")
    suspend fun getForDate(date: String): List<DailyUsage>

    /** 查询有历史记录的所有包名（去重） */
    @Query("SELECT DISTINCT packageName FROM daily_usage")
    fun getDistinctPackagesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT packageName FROM daily_usage")
    suspend fun getDistinctPackages(): List<String>

    /** 查询最近 N 天内有没有某日期的记录（用于补抓） */
    @Query("SELECT DISTINCT date FROM daily_usage WHERE date >= :since ORDER BY date ASC")
    suspend fun getDatesWithDataSince(since: String): List<String>
}
