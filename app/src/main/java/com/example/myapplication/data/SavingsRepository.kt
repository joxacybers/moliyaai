package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.db.SavingsGoalDao
import com.example.myapplication.data.db.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.savingsDataStore by preferencesDataStore(name = "savings_prefs")

class SavingsRepository(
    private val dao: SavingsGoalDao,
    private val context: Context,
) {
    val allGoals: Flow<List<SavingsGoalEntity>> = dao.getAll()

    suspend fun getById(id: Long): SavingsGoalEntity? = dao.getById(id)

    suspend fun addGoal(goal: SavingsGoalEntity): Long = dao.insert(goal)

    suspend fun updateGoal(goal: SavingsGoalEntity) = dao.update(goal)

    suspend fun deleteGoal(id: Long) = dao.delete(id)

    suspend fun addFunds(goalId: Long, amount: Long) {
        val goal = dao.getById(goalId) ?: return
        val newAmount = (goal.currentAmount + amount).coerceAtLeast(0)
        val completed = newAmount >= goal.targetAmount
        dao.update(goal.copy(currentAmount = newAmount, isCompleted = completed))
    }

    suspend fun withdrawFunds(goalId: Long, amount: Long) {
        val goal = dao.getById(goalId) ?: return
        val newAmount = (goal.currentAmount - amount).coerceAtLeast(0)
        val completed = newAmount >= goal.targetAmount
        dao.update(goal.copy(currentAmount = newAmount, isCompleted = completed))
    }

    // --- Emergency Fund (Moliyaviy yostiqcha) ---
    val emergencyFundBalance: Flow<Long> = context.savingsDataStore.data.map { it[KEY_EMERGENCY_BALANCE] ?: 0L }
    val emergencyFundPercent: Flow<Int> = context.savingsDataStore.data.map { it[KEY_EMERGENCY_PERCENT] ?: 10 }

    suspend fun addEmergencyFunds(amount: Long) {
        context.savingsDataStore.edit {
            val current = it[KEY_EMERGENCY_BALANCE] ?: 0L
            it[KEY_EMERGENCY_BALANCE] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun withdrawEmergencyFunds(amount: Long) {
        context.savingsDataStore.edit {
            val current = it[KEY_EMERGENCY_BALANCE] ?: 0L
            it[KEY_EMERGENCY_BALANCE] = (current - amount).coerceAtLeast(0)
        }
    }

    suspend fun setEmergencyFundPercent(percent: Int) {
        context.savingsDataStore.edit {
            it[KEY_EMERGENCY_PERCENT] = percent.coerceIn(1, 50)
        }
    }

    companion object {
        private val KEY_EMERGENCY_BALANCE = longPreferencesKey("emergency_fund_balance")
        private val KEY_EMERGENCY_PERCENT = intPreferencesKey("emergency_fund_percent")
    }
}
