package com.example.myapplication.data

import com.example.myapplication.data.db.TransactionDao
import com.example.myapplication.data.db.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {

    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()

    suspend fun getAll(): List<TransactionEntity> = dao.getAll()

    suspend fun get(id: Long): TransactionEntity? = dao.getById(id)

    suspend fun save(transaction: TransactionEntity): Long = dao.upsert(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    suspend fun insertAll(transactions: List<TransactionEntity>) = dao.insertAll(transactions)

    suspend fun replaceAll(transactions: List<TransactionEntity>) = dao.replaceAll(transactions)

    suspend fun deleteAll() = dao.deleteAll()
}
