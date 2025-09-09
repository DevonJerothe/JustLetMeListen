package com.devonjerothe.justletmelisten

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import com.devonjerothe.justletmelisten.di.serviceContainer
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
