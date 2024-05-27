package com.priceoye.justplugin.monetization.app_open

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.priceoye.justplugin.monetization.appModules
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

val No_Ads = false
var appInPauseState = false

class BaseApp : Application() {

    var activityRef: Activity? = null
    private val appOpenCommunicator: AppOpenCommunicator by inject()

    override fun onCreate() {
        super.onCreate()
        appContext = this
        startKoin {
            androidContext(applicationContext)
            androidLogger()
            modules(appModules)
        }

        initializeApp()
    }

    companion object {
        var appContext: Context? = null
    }

    fun initializeApp() {
        FirebaseApp.initializeApp(this)
        appOpenCommunicator.initAppClass(this)
    }


}