package com.priceoye.justplugin.monetization.native_ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.priceoye.justplugin.monetization.commons.getNativeAdsList
import com.priceoye.justplugin.monetization.commons.getNativeModelByKey
import com.priceoye.justplugin.monetization.commons.isNativeBlocked
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.priceoye.justplugin.monetization.Constants.isDebugMode
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.InternetController
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.commons.AdsRules
import com.priceoye.justplugin.monetization.inter.toBoolean

class NewNativeAdsManager(
    private val context: Context,
    private val prefs: MyPrefs,
    private val adsRules: AdsRules,
    private val internetController: InternetController,
) {


    val nativeAdsState = HashMap<String, NativeStateModel?>()
    val listener = HashMap<String, NativeAdListeners?>()

    fun String.isAdLoaded() = nativeAdsState[this]?.nativeAd != null

    fun String.isAdRequesting() = nativeAdsState[this]?.alreadyLoading ?: false

    fun setListener(key: String, nativeAdListeners: NativeAdListeners? = null) {
        listener[key] = nativeAdListeners
    }

    fun String.getListener() = listener[this]

    fun getBackupAd(): NativeStateModel? {
        val nativeAdsPlanList = getNativeAdsList(prefs)
        return nativeAdsState.toList().firstOrNull {
            val index = nativeAdsPlanList.indexOfFirst { native ->
                it.first == native.adKey && native.isBackupAd.toBoolean()
            }
            it.second?.nativeAd != null && (index != -1)
        }?.second
    }


    private var onAdClickListener: OnAdClickListener? = null

    fun loadAndShowNativeAd(
        context: Activity,
        key: String,
        canBeDestroyed: Boolean,
        listeners: NativeAdListeners? = null,
        onAdClicked: OnAdClickListener? = null
    ) {
        onAdClickListener = onAdClicked
        logError("loadAndShowNativeAd function Called $key")
        setListener(key, listeners)
        if (key.isNativeBlocked()) {
            logError("Native Ad Is Blocked (key=$key)")
            listener[key]?.onFailed()
            return
        }
        key.canBeDestroyed(canBeDestroyed)
        key.getNativeModelByKey(prefs)?.let { nativeAd ->
            if (nativeAd.adEnabled.toBoolean().not()) {
                logError("Ad Not Enabled ${nativeAd.adEnabled.toBoolean()}")
                return
            }
            if (key.isAdLoaded()) {
                logError("Native Ad Is Already Loaded")
                if (key.getListener() != null) {
                    key.onAdLoaded()
                }
                return
            }
            val backup = getBackupAd()
            if (backup?.nativeAd != null) {
                logError("Back Ad Found For $key")
                key.setNativeAdForKey(backup.nativeAd)
                if (key.getListener() != null) {
                    key.onAdLoaded()
                }
                nativeAdsState[backup.key] = null
                loadNativeAd(context, key)
                loadNativeAd(context, backup.key)
                return
            }
            loadNativeAd(context, key)
        }


    }

    fun loadNativeAd(
        context: Activity,
        key: String
    ) {
        loadAd(
            key,
            context,
            onFailure = {
                if (key.getListener() != null) {
                    key.onAdFailed()
                }
            },
            onLoaded = {
                if (key.getListener() != null) {
                    key.onAdLoaded()
                }
            }
        )
    }

    private fun String.onAdLoaded() {
        Log.d("cvv", "onAdLoaded:$this Destroy:${nativeAdsState[this]?.adDestroyAble} ")
        val nativeAd = nativeAdsState[this]?.nativeAd
        nativeAdsState[this] = null
        listener[this]?.onLoaded(nativeAd)
        listener[this] = null
    }

    private fun String.onAdFailed() {
        Log.d("cvv", "onAdFailed: Native $this")
        listener[this]?.onFailed()
        listener[this] = null
        nativeAdsState[this] = null
    }


    private fun loadAd(
        key: String,
        context: Activity,
        onFailure: () -> Unit,
        onLoaded: () -> Unit,
    ) {
        if (key.isNativeBlocked()) {
            logError("Native Ad Is Blocked (key=$key)")
            onFailure.invoke()
            return
        }
        if (internetController.isInternetConnected.not()) {
            logError("Internet Not Connected", key)
            onFailure.invoke()
            return
        }
        key.getNativeModelByKey(prefs)?.let { nativeAd ->
            if (adsRules.canShowNative.not()) {
                logError("Cannot Show Native rues")
                onFailure.invoke()
                return
            }
            if (nativeAd.adEnabled.toBoolean().not()) {
                logError("Native Ad Is Not Enabled")
                onFailure.invoke()
                return
            }
            if (key.isAdLoaded()) {
                logError("Native Ad Is Already Loaded")
                onLoaded.invoke()
                return
            }
            if (key.isAdRequesting()) {
                logError("Ad Is Already Requesting")
                return
            }
            key.setRequestingTrue()
            val adBuilder = AdLoader.Builder(context, nativeAd.adId)
            toast("Native Ad Requested(key=$key)")
            logError("Native Ad Requested(key=$key)")
            Log.d("cvv", "Native Ad Requested(key=$key) Ad Id: ${nativeAd.adId} ")
            adBuilder.forNativeAd { ad: NativeAd ->
                logError("Native Ad loaded (Key=$key)")
                toast("Native Ad loaded (Key=$key)")
                key.setNativeAdForKey(ad)
                onLoaded.invoke()
            }
            adBuilder.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logError("Native Ad Failed ${adError.code}:${adError.message}")
                    toast("Native Ad Failed(key=$key) ${adError.code}:${adError.message}")

                    val backup = getBackupAd()
                    if (backup?.nativeAd != null) {
                        key.setNativeAdForKey(backup.nativeAd)
                        nativeAdsState[backup.key] = null
                        loadNativeAd(context, backup.key)
                        onLoaded.invoke()
                        return
                    }
                    key.setNativeAdForKey(null)
                    onFailure.invoke()
                }

                override fun onAdLoaded() {
                }

                override fun onAdClicked() {
                    onAdClickListener?.onAdClicked(key)
                }
            })
            adBuilder.withNativeAdOptions(getNativeAdOptions(context))
            val adLoader = adBuilder.build()
            adLoader.loadAd(AdRequest.Builder().build())

        } ?: run {
            logError("No Native Ad Model Found $key")
            onFailure.invoke()
        }
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

    fun toast(msg: String) {
        if (isDebugMode) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun String.setNativeAdForKey(nativeAd: NativeAd? = null) {
        if (nativeAdsState[this] == null) {
            nativeAdsState[this] = NativeStateModel(
                key = this, nativeAd = nativeAd, alreadyLoading = false
            )
        } else if (nativeAdsState[this] != null) {
            nativeAdsState[this] = nativeAdsState[this]!!.copy(
                key = this, nativeAd = nativeAd, alreadyLoading = false
            )
        }
    }

    fun destroyNativeAd(key: String) {
        logError("Ad Being Destroyed $key")
        listener[key] = null
        nativeAdsState[key] = null
        /*nativeAdsState[key]?.let {
            if (it.adDestroyAble) {
                nativeAdsState[key] = null
            } else {
                nativeAdsState[key] = nativeAdsState[key]!!.copy(
                    alreadyLoading = false,
                )
            }
        }*/

    }

    private fun String.setRequestingTrue(requesting: Boolean = true) {
        if (nativeAdsState[this] == null) {
            nativeAdsState[this] = NativeStateModel(
                key = this, alreadyLoading = requesting
            )
        } else if (nativeAdsState[this] != null) {
            nativeAdsState[this] = nativeAdsState[this]!!.copy(
                key = this, alreadyLoading = requesting
            )
        }
    }

    fun getNativeAd(key: String): NativeAd? {
        return nativeAdsState[key]?.nativeAd
    }

    private fun String.canBeDestroyed(canBeDestroyed: Boolean) {
        if (nativeAdsState[this] == null) {
            nativeAdsState[this] = NativeStateModel(adDestroyAble = canBeDestroyed, key = this)
        } else if (nativeAdsState[this] != null) {
            nativeAdsState[this] = nativeAdsState[this]!!.copy(adDestroyAble = canBeDestroyed)
        }
    }
}
