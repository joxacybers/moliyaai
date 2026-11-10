package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.data.model.Account
import kotlinx.serialization.Serializable

enum class BillingCycle {
    MONTHLY,
    YEARLY
}

@Serializable
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Amount in UZS (so'm) */
    val amount: Long,
    /** Original display price text if entered in USD or another format (e.g. "$4.99" or "15 000 so'm") */
    val displayPrice: String = "",
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    /** Day of month (1..31) when payment renews */
    val billingDay: Int = 1,
    val account: Account = Account.CARD,
    /** Hex color or preset key */
    val colorHex: Long = 0xFF1976D2,
    val isActive: Boolean = true,
    val lastPaidEpochMonth: Long = 0,
)
