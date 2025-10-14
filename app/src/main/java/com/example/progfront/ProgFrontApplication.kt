package com.example.progfront

import android.app.Application
import com.example.progfront.data.remote.RetrofitClient

class ProgFrontApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitClient with context so it can use the AuthInterceptor
        RetrofitClient.initialize(this)
    }
}

