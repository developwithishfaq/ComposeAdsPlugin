package com.priceoye.justplugin.monetization.remote_config

import android.util.Log
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.priceoye.justplugin.monetization.Constants.isDebugMode
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.app_open.AppOpenManager
import com.priceoye.justplugin.monetization.commons.CanGoListener
import com.priceoye.justplugin.monetization.commons.MainAdsPlan
import com.priceoye.justplugin.monetization.commons.getAdsPlanFromJson
import com.priceoye.justplugin.monetization.inter.MyInterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


var isRemoteConfigsAssigned = false
var SPLASH_TIME = 10_000L


class RemoteConfigController(
    private val prefs: MyPrefs,
    private val interAdManager: MyInterManager,
    private val appOpenManager: AppOpenManager
) {

    private var listener: CanGoListener? = null

    private var canRequestConfig = true


    fun fetchAdConfigs(canGoListener: CanGoListener) {
        listener = canGoListener
        if (!canRequestConfig) {
            return
        }
        startConfigHandler()
        try {
            canRequestConfig = false
            val remoteConfig = FirebaseRemoteConfig.getInstance()
//            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            if (isDebugMode) {
                remoteConfig.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .build()
                )
            }
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        assignConfigValues(remoteConfig)
                    } else {
                        logError("Completed not success ${it.exception?.message}", error = true)
                        youCanGo()
                    }
                }.addOnFailureListener {
                    logError("Exception In Config OnFailureListener ${it.message}", error = true)
                    youCanGo()
                }.addOnCanceledListener {
                    logError("Exception In Config On Cancelled", error = true)
                    youCanGo()
                }
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    try {
                        assignConfigValues(remoteConfig)
                    } catch (e: Exception) {
                        logError("Exception In Config onUpdate ${e.message}")
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    logError("Exception In Config onError ${error.message}")
                }
            })
        } catch (e: Exception) {
            logError("Exception In Config ${e.message}")
            youCanGo()
        }
    }

    private fun startConfigHandler() {/*
        Handler(Looper.getMainLooper()).postDelayed({
            if (listener != null) {
                youCanGo()
            }
        }, SPLASH_TIME)*/
    }

    private fun youCanGo() {
        logError("Config youCanGo Called ", error = true)
        listener?.canGo()
        listener = null
        canRequestConfig = true
    }

    private fun assignConfigValues(remoteConfig: FirebaseRemoteConfig) {
        logError("assignConfigValues: CALLED ")
        with(remoteConfig) {

            try {
                if (BuildConfig.DEBUG.not()) {
                    val interAdsJson = getString("mainAds")
                    isRemoteConfigsAssigned = true
                    prefs.adsJson = interAdsJson
                    logError("Release Publish")
                    publishAdsJson(interAdsJson)
                    youCanGo()
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        getAdsPlanFromJson()?.let {
                            logError("Debug Publish")
                            prefs.adsJson = it
                            publishAdsJson(prefs.adsJson)
                            withContext(Dispatchers.Main) {
                                youCanGo()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logError("assignConfigValues Exception:${e.message}  ")
                youCanGo()
            }
        }
    }

    private var resultsPublished = false

    private fun publishAdsJson(json: String) {
        if (!resultsPublished) {
            Log.d("publishAdsJson:", "publishAdsJson: $json")
            json.toModel<MainAdsPlan>()?.let {
                interAdManager.setInterAds(it.interstitialAds?.ads ?: emptyList())
                appOpenManager.setAppOpenAds(it.appOpenAds?.ads ?: emptyList())
            }
            resultsPublished = true
        }
    }

    fun reset() {
        youCanGo()
    }

}


inline fun <reified T> String.toModel(): T? {
    return try {
        Json.decodeFromString(this)
    } catch (_: Exception) {
        null
    }
}

