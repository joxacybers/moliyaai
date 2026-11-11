package com.example.myapplication.data

import com.example.myapplication.data.db.DebtDao
import com.example.myapplication.data.db.DebtEntity
import com.example.myapplication.data.db.DebtType
import kotlinx.coroutines.flow.Flow

class DebtRepository(private val dao: DebtDao) {
    val allDebts: Flow<List<DebtEntity>> = dao.getAll()

    fun getActiveByType(type: DebtType): Flow<List<DebtEntity>> = dao.getActiveByType(type)

    suspend fun getById(id: Long): DebtEntity? = dao.getById(id)

    suspend fun addDebt(debt: DebtEntity): Long = dao.insert(debt)

    suspend fun updateDebt(debt: DebtEntity) = dao.update(debt)

    suspend fun recordPayment(id: Long, additionalAmount: Long) {
        val debt = dao.getById(id) ?: return
        val newPaid = (debt.paidAmount + additionalAmount).coerceAtMost(debt.amount)
        val settled = newPaid >= debt.amount
        dao.update(debt.copy(paidAmount = newPaid, isSettled = settled))
    }

    suspend fun toggleSettled(id: Long) {
        val debt = dao.getById(id) ?: return
        val newSettled = !debt.isSettled
        val newPaid = if (newSettled) debt.amount else debt.paidAmount
        dao.update(debt.copy(isSettled = newSettled, paidAmount = newPaid))
    }

    suspend fun deleteDebt(id: Long) = dao.delete(id)
}
