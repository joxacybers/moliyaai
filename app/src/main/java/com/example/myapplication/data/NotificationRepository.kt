package com.example.myapplication.data

import com.example.myapplication.data.db.AppNotificationDao
import com.example.myapplication.data.db.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val dao: AppNotificationDao,
) {
    fun observeAll(): Flow<List<AppNotificationEntity>> = dao.observeAll()

    fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    suspend fun getAll(): List<AppNotificationEntity> = dao.getAll()

    suspend fun getById(id: Long): AppNotificationEntity? = dao.getById(id)

    suspend fun insert(entity: AppNotificationEntity): Long = dao.insert(entity)

    suspend fun update(entity: AppNotificationEntity) = dao.update(entity)

    suspend fun markAsRead(id: Long) = dao.markAsRead(id)

    suspend fun markAllAsRead() = dao.markAllAsRead()

    suspend fun markActionCompleted(id: Long) = dao.markActionCompleted(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()
}
