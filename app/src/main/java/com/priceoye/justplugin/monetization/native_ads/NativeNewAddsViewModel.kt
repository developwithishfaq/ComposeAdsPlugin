package com.priceoye.justplugin.monetization.native_ads

import android.app.Activity
import android.view.View
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.commons.AdsRules

interface NativeAdListeners {
    fun onLoaded(nativeAd: NativeAd?) {}
    fun onLoadedBanner(adView: AdView?) {}
    fun onFailed()
}

data class NativeStateModel(
    val key: String,
    val nativeAd: NativeAd? = null,
    val backupAd: Boolean = false,
    val alreadyLoading: Boolean = false,
    val adDestroyAble: Boolean = true,
)

interface OnAdClickListener {
    fun onAdClicked(key: String)
}

class NativeNewAddsViewModel(
    private val prefs: MyPrefs,
    private val adsRules: AdsRules,
    private val nativeAdsManager: NewNativeAdsManager
) : ViewModel() {


    val nativeAds = HashMap<String, NativeAd?>()
    val reloadingAds = mutableListOf<String>()
    val listeners = HashMap<String, NewNativeAdListener?>()


    interface NewNativeAdListener {
        fun onLoaded(nativeAd: NativeAd)
        fun onFailure()
    }

    fun String.onAdLoaded(nativeAd: NativeAd) {
        listeners[this]?.onLoaded(nativeAd)
    }

    fun String.onFailure() {
        listeners[this]?.onFailure()
    }

    private var lastReloaded = 0L

    private var alreadyRequesting = false


    fun showAd(
        key: String,
        context: Activity,
        newNativeAdListener: NewNativeAdListener? = null,
        canBeDestroyed: Boolean = true,
        reloadAd: Boolean = false
    ) {
        logError("Native Ad ($key) Show Ad Function Called , isForReload=$reloadAd")
        if (reloadAd) {

            if (reloadingAds.contains(key)) {
                return
            }
        }
        reloadingAds.add(key)
        newNativeAdListener?.let {
            listeners[key] = newNativeAdListener
        }
        if (reloadAd.not()) {
            if (nativeAds[key] != null) {
                nativeAds[key]?.let { newNativeAdListener?.onLoaded(it) }
                return
            }
        }
        lastReloaded = System.currentTimeMillis()
        nativeAdsManager.loadAndShowNativeAd(
            context,
            key,
            canBeDestroyed,
            onAdClicked = object : OnAdClickListener {
                override fun onAdClicked(key: String) {
                    if (nativeAds[key] != null) {
                        reloadAd(key, context)
                    }
                }
            },
            listeners = object : NativeAdListeners {
                override fun onLoaded(nativeAd: NativeAd?) {
                    if (context.canShowAd()) {
                        nativeAd?.let {
                            nativeAds[key] = it
                            key.onAdLoaded(nativeAd)
                            nativeAdsManager.loadNativeAd(context, key)
                        } ?: run {
                            nativeAds[key] = null
                            key.onFailure()
                        }
                    }
                    reloadingAds.remove(key)
                }

                override fun onFailed() {
                    if (context.canShowAd()) {
                        key.onFailure()
                    }
                    reloadingAds.remove(key)
                }
            })
    }

    fun justLoadNativeAd(key: String, context: Activity) {
        nativeAdsManager.loadNativeAd(context, key)
    }

    fun destroyAd(key: String) {
        nativeAds[key] = null
        nativeAdsManager.destroyNativeAd(key)
    }

    fun getPref(): MyPrefs {
        return prefs
    }


    private fun getNativeAdOptions(context: Activity): NativeAdOptions {
        val builder = NativeAdOptions.Builder()
        if (context.window.decorView.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            builder.setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
        }
        return builder.setVideoOptions(
            VideoOptions.Builder().setStartMuted(true).build()
        ).build()
    }

    fun getNativeAd(key: String): NativeAd? {
        return nativeAds[key]
    }

    fun reloadAd(key: String, activity: Activity) {
        showAd(key, activity, null, true, reloadAd = true)
    }


}

fun Activity.canShowAd() = isFinishing.not() && isDestroyed.not()

