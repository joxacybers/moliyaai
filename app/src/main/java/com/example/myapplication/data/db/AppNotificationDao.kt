package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT * FROM app_notifications ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<AppNotificationEntity>

    @Query("SELECT * FROM app_notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AppNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppNotificationEntity): Long

    @Update
    suspend fun update(entity: AppNotificationEntity)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("UPDATE app_notifications SET isActionCompleted = 1, isRead = 1 WHERE id = :id")
    suspend fun markActionCompleted(id: Long)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAll()
}
