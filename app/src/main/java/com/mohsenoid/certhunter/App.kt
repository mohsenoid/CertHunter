package com.mohsenoid.certhunter

import android.app.Application
import com.mohsenoid.certhunter.di.appModule
import com.mohsenoid.klogx.KLogLogger
import com.mohsenoid.klogx.logcat.KLogLogcatAppender
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        KLogLogger.registerAppender(KLogLogcatAppender { _, _, _, _ -> BuildConfig.DEBUG })
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
