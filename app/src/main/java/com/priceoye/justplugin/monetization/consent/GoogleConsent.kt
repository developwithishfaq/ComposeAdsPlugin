package com.priceoye.justplugin.monetization.consent

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.priceoye.justplugin.monetization.Constants
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.commons.CanGoListener
import java.util.concurrent.atomic.AtomicBoolean


class GoogleConsent(
    private val context: Context, private val prefs: MyPrefs
) {
    private val isSdkInitialized = AtomicBoolean(false)

    private var consentInformation = UserMessagingPlatform.getConsentInformation(context)

    val canRequestAds: Boolean
        get() = if (Constants.isDebugMode) {
            true
        } else {
            consentInformation.canRequestAds()
        }


    private val debugSettings = ConsentDebugSettings.Builder(context)
        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        .addTestDeviceHashedId("F35C4591A4D8063274BE764E6A6FDD96").build()

    private val params: ConsentRequestParameters =
        ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()

    private var listener: CanGoListener? = null

    private fun youCanGo() {
        listener?.canGo()
        listener = null
    }

    fun reset() {
        youCanGo()
    }

    private var alreadyRequested = false

    fun showConsent(activity: Activity, canGoListener: CanGoListener? = null) {
        if (alreadyRequested) {
            return
        }
        Log.d("cvv", "showConsent canRequestAds:$canRequestAds ")
        listener = canGoListener
        if (canRequestAds) {
            initAdMob()
            youCanGo()
            return
        }
        if (prefs.appPurchased) {
            youCanGo()
            return
        }
        alreadyRequested = true
        startHandler()
        consentInformation.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                activity
            ) {
                alreadyRequested = false
                if (it != null) {
                    Log.d("cvv", "showConsent: Error :${it.message} ")
                }
                if (consentInformation.canRequestAds()) {
                    initAdMob()
                }
                youCanGo()
            }
        }, { e ->

            alreadyRequested = false
            if (canRequestAds) {
                initAdMob()
            }
            Log.d("cvv", "showConsent: error ${e.errorCode} $canRequestAds")
//            Log.d("cvv", "showConsent: code ${e.errorCode},$canRequestAds ")
//            Log.d("cvv", "showConsent: exception ${e}")
            youCanGo()
        })
        if (consentInformation.canRequestAds()) {
            alreadyRequested = false
            initAdMob()
            youCanGo()
        }
    }

    private fun startHandler() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (listener != null) {
                youCanGo()
            }
        }, 35_000)
    }

    private fun initAdMob() {
        if (isSdkInitialized.getAndSet(true)) {
            return
        }
        try {
            MobileAds.initialize(context) {}
        } catch (_: NoSuchMethodError) {
        } catch (_: Exception) {
        }
    }

}