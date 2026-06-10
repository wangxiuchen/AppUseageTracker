package com.appspy.data.db.entity

import androidx.room.Entity

/**
 * 每个 app 每天一行，趋势图的主数据来源。
 * 联合主键：date + packageName。
 */
@Entity(
    tableName = "daily_usage",
    primaryKeys = ["date", "packageName"]
)
data class DailyUsage(
    /** 日期，格式 yyyy-MM-dd */
    val date: String,
    val packageName: String,
    /** 当天 ACTIVITY_RESUMED 事件数（来回切算多次，不去重） */
    val openCount: Int,
    /** 当天前台总时长（毫秒） */
    val totalForegroundMs: Long,
    /** 最后一次更新时间（epoch ms） */
    val lastUpdated: Long = System.currentTimeMillis()
)
