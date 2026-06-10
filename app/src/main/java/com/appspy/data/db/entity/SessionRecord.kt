package com.appspy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每次前台使用的会话明细（可选，供未来细粒度分析使用）。
 */
@Entity(tableName = "session_record")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    /** 所属日期，格式 yyyy-MM-dd */
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long
)
