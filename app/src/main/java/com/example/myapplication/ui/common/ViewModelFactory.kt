package com.example.myapplication.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.FinanceApp
import com.example.myapplication.di.AppContainer

/** Access to the app-wide DI container from composables (used inside `viewModel { }`). */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as FinanceApp).container
