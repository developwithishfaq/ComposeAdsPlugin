package com.priceoye.justplugin.monetization.bannerAds

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.priceoye.justplugin.monetization.commons.AdsRules
import com.priceoye.justplugin.monetization.commons.getBannerModelByKey
import com.priceoye.justplugin.monetization.inter.toBoolean
import com.priceoye.justplugin.monetization.native_ads.NativeAdListeners
import com.priceoye.justplugin.monetization.native_ads.canShowAd
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.priceoye.justplugin.monetization.Constants.adsToast
import com.priceoye.justplugin.monetization.Constants.isDebugMode
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.InternetController
import com.priceoye.justplugin.monetization.MyPrefs
import java.util.UUID

data class BannerAdStateModel(
    val key: String,
    val bannerAd: AdView? = null,
    val alreadyLoading: Boolean = false,
    val adDestroyAble: Boolean = true,
)

var bannerAdsList: HashMap<String, BannerAdStateModel?> = hashMapOf()
val bannerAdsListener = HashMap<String, NativeAdListeners?>()

class BannerAdViewModel(
    private val prefs: MyPrefs,
    private val adsRules: AdsRules,
    private val controller: InternetController
) : ViewModel() {


    fun String.isAdLoaded() = bannerAdsList[this]?.bannerAd != null

    fun String.isAdRequesting() = bannerAdsList[this]?.alreadyLoading ?: false

    fun showAd(
        key: String,
        context: Activity,
        onFailure: () -> Unit,
        onLoaded: (AdView?) -> Unit,
        canBeDestroyed: Boolean = true
    ) {
        if (bannerAdsList[key]?.bannerAd != null) {
            bannerAdsList[key]?.let { onLoaded.invoke(it.bannerAd) }
            return
        }
        loadBannerAd(context, key, canBeDestroyed, object : NativeAdListeners {
            override fun onLoadedBanner(adView: AdView?) {
                if (context.canShowAd()) {
                    adView?.let {
                        onLoaded.invoke(it)
                    } ?: run {
                        onFailure.invoke()
                    }
                }
            }

            override fun onFailed() {
                if (context.canShowAd()) {
                    onFailure.invoke()
                }
            }
        })

    }

    fun destroyBannerAd(key: String) {
        bannerAdsListener[key] = null
        bannerAdsList[key]?.let {
            if (it.adDestroyAble) {
                bannerAdsList[key] = null
            } else {
                bannerAdsList[key] = bannerAdsList[key]!!.copy(
                    alreadyLoading = false,
                )
            }
        }

    }


    fun justLoadBannerAd(key: String, context: Activity) {
        loadBannerAd(context, key, true)
    }

    fun loadBannerAd(
        context: Activity,
        key: String,
        canBeDestroyed: Boolean,
        listeners: NativeAdListeners? = null
    ) {
        setListener(key, listeners)
        key.canBeDestroyed(canBeDestroyed)
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
        Log.d("cvv", "onAdLoaded:$this Destroy:${bannerAdsList[this]?.adDestroyAble} ")
        val bannerAd = bannerAdsList[this]?.bannerAd
        bannerAdsListener[this]?.onLoadedBanner(bannerAd)
        bannerAdsListener[this] = null
        bannerAdsList[this] = bannerAdsList[this]!!.copy(
            alreadyLoading = false,
            bannerAd = bannerAd
        )
    }

    private fun String.onAdFailed() {
        Log.d("cvv", "onAdFailed:Banner ")
        bannerAdsListener[this]?.onFailed()
        bannerAdsListener[this] = null
        bannerAdsList[this] = null
    }


    fun setListener(key: String, bannerAdListeners: NativeAdListeners? = null) {
        bannerAdsListener[key] = bannerAdListeners
    }

    fun String.getListener() = bannerAdsListener[this]


    private fun loadAd(
        key: String,
        context: Activity,
        onFailure: () -> Unit,
        onLoaded: () -> Unit,
    ) {
        key.getBannerModelByKey(prefs)?.let { adModel ->
            if (adsRules.canShowBanner.not()) {
                logError("Cannot Show Banner rues")
                onFailure.invoke()
                return
            }
            if (adModel.adEnabled.toBoolean().not()) {
                logError("Banner Ad Is Not Enabled")
                onFailure.invoke()
                return
            }
            if (key.isAdLoaded()) {
                logError("Banner Ad Is Already Loaded")
                onLoaded.invoke()
                return
            }
            if (key.isAdRequesting()) {
                return
            }
            key.setRequestingTrue()
            val bannerAd = AdView(context)

            val extras = Bundle()
            if (adModel.isCollapsible()) {
                extras.putString("collapsible", "bottom")
                extras.putString("collapsible_request_id", UUID.randomUUID().toString());
            }

            bannerAd.apply {
                this.adUnitId = adModel.adId
                this.setAdSize(adModel.getSize())
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        "Banner Loaded".adsToast(context)
                        key.setBannerAdForKey(bannerAd)
                        onLoaded.invoke()
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        "Banner Failed to Load ${p0.code}:${p0.message}".adsToast(context)
                        logError("Banner Failed to Load ${p0.code}:${p0.message}", key)
                        onFailure.invoke()
                    }
                }
                if (controller.isInternetConnected) {
                    "Banner Ad Requested".adsToast(context)
                    logError("Banner Ad Requested", key)
                    val adRequest = AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                        .build()
                    loadAd(adRequest)
                } else {
                    logError("Banner Ad Loading Is Revoked Due to Internet", key, true)
                    onFailure.invoke()
                }
            }
        } ?: run {
            onFailure.invoke()
        }
    }

    fun String.setBannerAdForKey(bannerAd: AdView? = null) {
        if (bannerAdsList[this] == null) {
            bannerAdsList[this] = BannerAdStateModel(
                key = this, bannerAd = bannerAd, alreadyLoading = false
            )
        } else if (bannerAdsList[this] != null) {
            bannerAdsList[this] = bannerAdsList[this]!!.copy(
                key = this, bannerAd = bannerAd, alreadyLoading = false
            )
        }
    }


    fun removeFromLoading(key: String) {
        bannerAdsList[key] = null
    }

    fun getPref() = prefs


    private fun String.canBeDestroyed(canBeDestroyed: Boolean) {
        if (bannerAdsList[this] == null) {
            bannerAdsList[this] = BannerAdStateModel(adDestroyAble = canBeDestroyed, key = this)
        } else if (bannerAdsList[this] != null) {
            bannerAdsList[this] = bannerAdsList[this]!!.copy(adDestroyAble = canBeDestroyed)
        }
    }


    private fun String.setRequestingTrue(requesting: Boolean = true) {
        if (bannerAdsList[this] == null) {
            bannerAdsList[this] = BannerAdStateModel(
                key = this, alreadyLoading = requesting
            )
        } else if (bannerAdsList[this] != null) {
            bannerAdsList[this] = bannerAdsList[this]!!.copy(
                key = this, alreadyLoading = requesting
            )
        }
    }

    fun toast(msg: String, context: Context) {
        if (isDebugMode) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun getBannerAd(key: String): AdView? {
        return bannerAdsList[key]?.bannerAd
    }


}
