package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0L,
    val iconKey: String = "car",
    val colorHex: Long = 0xFF1976D2,
    val targetDateEpochMillis: Long? = null,
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
) {
    val remainingAmount: Long
        get() = (targetAmount - currentAmount).coerceAtLeast(0)

    val progress: Float
        get() = if (targetAmount > 0) (currentAmount.toFloat() / targetAmount).coerceIn(0f, 1f) else 0f

    val percent: Int
        get() = (progress * 100).toInt()
}
