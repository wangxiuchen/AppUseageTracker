package com.appspy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用元信息：包名 → 显示名。
 * 图标不入库，运行时通过 PackageManager 现取。
 */
@Entity(tableName = "app_meta")
data class AppMeta(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val lastRefreshed: Long = System.currentTimeMillis()
)
