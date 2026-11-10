package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import java.time.LocalDate

@Entity(tableName = "transactions", indices = [Index("epochDay")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Amount in so'm, always positive. Direction is defined by [type]. */
    val amount: Long,
    val type: TransactionType,
    val category: Category,
    val account: Account = Account.CARD,
    /** [LocalDate.toEpochDay] of the transaction date. */
    val epochDay: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

val TransactionEntity.date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
