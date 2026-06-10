package com.appspy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appspy.data.db.entity.SessionRecord

@Dao
interface SessionRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionRecord>)

    @Query("SELECT * FROM session_record WHERE date = :date AND packageName = :pkg ORDER BY startTime ASC")
    suspend fun getForDateAndApp(date: String, pkg: String): List<SessionRecord>

    /** 删除某日期的某 app 所有会话（落库前清理旧数据） */
    @Query("DELETE FROM session_record WHERE date = :date AND packageName = :pkg")
    suspend fun deleteForDateAndApp(date: String, pkg: String)

    @Query("DELETE FROM session_record WHERE date = :date")
    suspend fun deleteForDate(date: String)
}
