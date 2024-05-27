package com.priceoye.justplugin.monetization.inter

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.commons.AdsRules

class InterstitialViewModel(
    private val adsRules: AdsRules,
    prefs: MyPrefs,
    private val newManager: MyInterManager
) : ViewModel() {

    fun loadInterAd(key: String) {
        newManager.loadInterAd(key)
    }

    fun showAd(
        context: Activity,
        key: String,
        showAtFirstClick: Boolean = false,
        onFree: (Boolean) -> Unit
    ) {
        newManager.showAd(
            context = context,
            key = key,
            forceInterAd = null,
            onFree = onFree,
            showAtFirstClick = showAtFirstClick
        )
    }

    fun showAdSplashIfAvailable(
        context: Activity,
        key: String,
        showAtFirstClick: Boolean = false,
        onFree: (Boolean) -> Unit
    ) {
        newManager.showAd(
            context = context,
            key = key,
            forceInterAd = null,
            onFree = onFree,
            showAtFirstClick = showAtFirstClick,
            showForceWithoutCounter = true
        )
    }

}

