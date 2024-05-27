package com.priceoye.justplugin.monetization.commons

import android.content.Context
import com.priceoye.justplugin.monetization.consent.GoogleConsent
import com.priceoye.justplugin.monetization.InternetController
import com.priceoye.justplugin.monetization.MyPrefs


class AdsRules(
    private val context: Context,
    private val prefs: MyPrefs,
    private val consent: GoogleConsent,
    private val internetController: InternetController
) {

    val canShowNative: Boolean
        get() {
            return commonEnableChecks
        }

    val canShowBanner: Boolean
        get() {
            return commonEnableChecks
        }

    val canShowInterstitial: Boolean
        get() {/*
            Log.d("cvv", "Show Inter: ${prefs.entryCount}")
            Log.d("cvv", "Common Enable: $commonEnableChecks")*/
            return commonEnableChecks

        }
    private val commonEnableChecks: Boolean
        get() {
            return consent.canRequestAds && prefs.appPurchased.not()
//                    && internetController.isInternetConnected
        }

}