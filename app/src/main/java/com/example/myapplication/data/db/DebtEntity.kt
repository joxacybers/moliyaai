package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class DebtType {
    /** Men qarz berdim (Menga qaytarilishi kerak / Olinadigan) */
    LENT,
    /** Men qarz oldim (Men qaytarishim kerak / To'lanadigan) */
    BORROWED
}

@Serializable
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val phoneNumber: String = "",
    val type: DebtType,
    val amount: Long,
    val paidAmount: Long = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val dueDateEpochMillis: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
) {
    val remainingAmount: Long
        get() = (amount - paidAmount).coerceAtLeast(0)
}
