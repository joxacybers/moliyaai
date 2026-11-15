package com.example.myapplication.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TransactionType,
    val iconKey: String = "other",
    val colorHex: Long = 0xFF1976D2,
)
