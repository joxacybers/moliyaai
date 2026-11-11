package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getForCategory(category: Category): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteForCategory(category: Category)
}
