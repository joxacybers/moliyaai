package com.example.myapplication.data

import com.example.myapplication.data.db.BudgetDao
import com.example.myapplication.data.db.BudgetEntity
import com.example.myapplication.data.model.Category
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val dao: BudgetDao) {
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAll()

    suspend fun getForCategory(category: Category): BudgetEntity? = dao.getForCategory(category)

    suspend fun setBudget(category: Category, limitAmount: Long) {
        val existing = dao.getForCategory(category)
        if (limitAmount <= 0) {
            if (existing != null) {
                dao.delete(existing.id)
            }
        } else {
            val entity = existing?.copy(limitAmount = limitAmount)
                ?: BudgetEntity(category = category, limitAmount = limitAmount)
            dao.insert(entity)
        }
    }

    suspend fun deleteBudget(id: Long) = dao.delete(id)
}
