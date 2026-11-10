package com.example.myapplication.data

import com.example.myapplication.data.db.SubscriptionDao
import com.example.myapplication.data.db.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val dao: SubscriptionDao) {

    fun observeAll(): Flow<List<SubscriptionEntity>> = dao.observeAll()

    suspend fun getAll(): List<SubscriptionEntity> = dao.getAll()

    suspend fun save(subscription: SubscriptionEntity): Long = dao.upsert(subscription)

    suspend fun delete(subscription: SubscriptionEntity) = dao.delete(subscription)

    suspend fun deleteAll() = dao.deleteAll()
}
