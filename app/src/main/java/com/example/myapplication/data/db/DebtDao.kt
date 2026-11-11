package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, dueDateEpochMillis ASC, createdAtEpochMillis DESC")
    fun getAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE type = :type AND isSettled = 0")
    fun getActiveByType(type: DebtType): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun delete(id: Long)
}
