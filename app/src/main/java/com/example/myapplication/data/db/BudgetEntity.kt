package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.data.model.Category
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: Category,
    val limitAmount: Long,
    val periodMonth: String = "",
)
