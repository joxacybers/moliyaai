package com.example.myapplication.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppRoute : NavKey

@Serializable
data object HomeTab : AppRoute

@Serializable
data object HistoryTab : AppRoute

@Serializable
data object SubscriptionsTab : AppRoute

@Serializable
data object StatsTab : AppRoute

@Serializable
data object AdvisorTab : AppRoute

@Serializable
data class AddEditRoute(val transactionId: Long = 0) : AppRoute

@Serializable
data object SettingsRoute : AppRoute

@Serializable
data object CategoryBudgetsRoute : AppRoute

@Serializable
data object DebtsRoute : AppRoute
