package com.appspy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appspy.data.db.entity.AppMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appMeta: AppMeta)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<AppMeta>)

    @Query("SELECT * FROM app_meta WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppMeta?

    @Query("SELECT * FROM app_meta ORDER BY appLabel ASC")
    fun getAllFlow(): Flow<List<AppMeta>>

    @Query("SELECT * FROM app_meta ORDER BY appLabel ASC")
    suspend fun getAll(): List<AppMeta>
}
