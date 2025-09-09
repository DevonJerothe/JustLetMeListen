package com.devonjerothe.justletmelisten

import android.app.Application
import com.devonjerothe.justletmelisten.core.di.serviceContainer
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class JustLetMeListen : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@JustLetMeListen)
            modules(serviceContainer)
        }
    }
}
