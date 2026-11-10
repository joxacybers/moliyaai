package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.AppContainer

class FinanceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
