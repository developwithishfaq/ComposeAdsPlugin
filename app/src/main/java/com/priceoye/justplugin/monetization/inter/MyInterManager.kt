package com.priceoye.justplugin.monetization.inter

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.priceoye.justplugin.monetization.Constants.isDebugMode
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.InternetController
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.commons.CounterAdsHelper
import com.priceoye.justplugin.monetization.commons.SimpleCounterHelper.getCurrentCounter
import com.priceoye.justplugin.monetization.commons.SimpleCounterHelper.incrementCounter
import com.priceoye.justplugin.monetization.commons.SimpleCounterHelper.resetCounter
import com.priceoye.justplugin.monetization.commons.isInterBlocked
import com.priceoye.justplugin.monetization.consent.GoogleConsent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface MyAdListener {
    fun onAdClosed(adShown: Boolean)
    fun onAdShown() {}
}


class MyInterManager(
    private val context: Context,
    private val internetController: InternetController,
    private val prefs: MyPrefs,
    private val counterAdsHelper: CounterAdsHelper,
    private val consent: GoogleConsent
) {
    data class InterAdModel(
        val adKey: String,
        val interAd: InterstitialAd? = null,
        val canRequestAd: Boolean = true,
        val counter: Int = 0,
        val adId: String = "",
        val listener: MyAdListener? = null
    )


    private val interAdsList = MutableStateFlow<List<InterAdModel>>(emptyList())


    private val listener = HashMap<String, MyAdListener?>()

    private fun String.interAlreadyRequesting(): Boolean {
        return interAdsList.value.firstOrNull { it.canRequestAd.not() && this == it.adKey } != null
    }

    private fun String.setCanRequestAd(canRequestAd: Boolean) {
        val list = interAdsList.value.toMutableList()
        val index = list.indexOfFirst {
            it.adKey == this
        }
        if (index != -1) {
            val model = list[index]
            list.removeAt(index)
            list.add(
                model.copy(
                    canRequestAd = canRequestAd
                )
            )
        } else {
            list.add(InterAdModel(this, null, canRequestAd))
        }
        interAdsList.update {
            list
        }
    }

    private fun String.getAdAvailable(): InterstitialAd? =
        interAdsList.value.firstOrNull { this == it.adKey }?.interAd

    private fun String.getModelByKey(): Pair<String, InterAdModel?> {
        var adKey = this
        val availableAd = interAdsList.value.firstOrNull {
            it.interAd != null
        }
        val interModel = interAdsList.value.firstOrNull { this == it.adKey }
        val model = if (interModel != null) {
            if (interModel.interAd != null) {
                adKey = interModel.adKey
                interModel
            } else if (availableAd?.interAd != null) {
                adKey = availableAd.adKey
                interModel.copy(
                    interAd = availableAd.interAd
                )
            } else {
                interModel
            }
        } else {
            null
        }
        return Pair(adKey, model)
    }

    fun String.setInterAd(interAdModel: InterstitialAd? = null, canRequestAd: Boolean = true) {
        val list = interAdsList.value.toMutableList()
        val index = list.indexOfFirst {
            it.adKey == this
        }
        if (index != -1) {
            val model = list[index]
            list.removeAt(index)
            list.add(
                model.copy(
                    interAd = interAdModel, canRequestAd = canRequestAd
                )
            )
        } else {
            list.add(InterAdModel(this, interAdModel, canRequestAd))
        }
        interAdsList.update {
            list
        }
    }

    fun showAd(
        context: Activity,
        key: String,
        forceInterAd: InterstitialAd? = null,
        showAtFirstClick: Boolean = false,
        onFree: (Boolean) -> Unit,
        showForceWithoutCounter: Boolean = false
    ) {
        if (key.isInterBlocked()) {
            logError("Ad Is Blocked (key=$key)")
            onFree.invoke(false)
            return
        }
        if (forceInterAd != null && showForceWithoutCounter) {
            setListener(key, object : MyAdListener {
                override fun onAdClosed(adShown: Boolean) {
                    onFree.invoke(adShown)
                }
            })
            setInterAdCallBacks(key, forceInterAd)
            forceInterAd.show(context)
        } else {
            val mainModel = key.getModelByKey()
            val adModel = mainModel.second

            if (adModel == null) {
                logError("No Ad Model Found Against This Key")
                onFree.invoke(false)
                return
            }
            if (adModel.interAd != null) {
                val adCounter = counterAdsHelper.getCurrentCounter(adModel.adKey)
                val shouldShowAd = showAtFirstClick && adCounter == 0
                if (adCounter >= adModel.counter || shouldShowAd) {
                    logError("Counter Reached Ad Must Show $key")
                    setListener(key, object : MyAdListener {
                        override fun onAdClosed(adShown: Boolean) {
                            if (adShown) {
                                mainModel.first.setInterAd(null, true)
                                if (shouldShowAd) {
                                    counterAdsHelper.incrementCounter(adModel.adKey)
                                } else {
                                    counterAdsHelper.resetCounter(adModel.adKey)
                                }
                            }
                            onFree.invoke(adShown)
                        }
                    })
                    setInterAdCallBacks(key, forceInterAd)
                    adModel.interAd.show(context)
                } else {
                    logError("Inter($key) Not Shown Counter Increase , Prev Counter:$adCounter ")
                    counterAdsHelper.incrementCounter(adModel.adKey)
                    onFree.invoke(false)
                }
            } else {
                counterAdsHelper.incrementCounter(adModel.adKey)
                logError("InterAd Not Available It Must Load a ad $key")
                loadInterAd(key)
                onFree.invoke(false)
            }
        }
    }

    fun loadInterAd(key: String) {
        key.getModelByKey().second ?: return
        val adAvailable = key.getAdAvailable()
        if (adAvailable != null) {
            logError("Ad Already Available for this key:$key")
            return
        }

        loadInterAdCoreDoNotUse(key, onLoaded = {
            key.setInterAd(it)
        }, onFailed = {
            key.setInterAd(null)
        })
    }


    private fun setListener(key: String, myAdListener: MyAdListener) {
        listener[key] = myAdListener
    }


    fun loadInterAdCoreDoNotUse(
        key: String, onLoaded: (InterstitialAd) -> Unit, onFailed: () -> Unit
    ) {
        if (key.isInterBlocked()) {
            logError("Ad Is Blocked (key=$key)")
            onFailed.invoke()
        }
        if (consent.canRequestAds.not()) {
            logError("Consent Not Allowed")
            onFailed.invoke()
            return
        }
        val mainModel = key.getModelByKey()
        val adModel = mainModel.second
        logError("LoadInterAdCore Model: $adModel, List Size:${interAdsList.value.size}")
        if (adModel == null) {
            onFailed.invoke()
            return
        }
        if (adModel.interAd != null) {
            logError("loadInterAdCore Ad Already available")
            onLoaded.invoke(adModel.interAd)
            return
        }
        if (internetController.isInternetConnected.not()) {
            logError("loadInterAdCore No Internet")
            onFailed.invoke()
            return
        }
        if (key.interAlreadyRequesting()) {
            logError("loadInterAdCore Ad Already Requesting")
            return
        }
        key.setCanRequestAd(false)
        logError("Inter Ad Requested $key")
        toast("Inter Ad Requested $key:${adModel.adId}")
        InterstitialAd.load(context,
            adModel.adId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    toast("Inter Ad Failed ($key) ${adError.code}:${adError.message}")
                    logError("Inter Ad Failed ($key) ${adError.code}:${adError.message},adId:${adModel.adId}")
                    onFailed.invoke()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    toast("Inter Ad Loaded ($key)")
                    logError("Inter Ad Loaded ($key)")
                    onLoaded.invoke(interstitialAd)
                }
            })

    }


    private fun setInterAdCallBacks(key: String, forceInterAd: InterstitialAd? = null) {
        val mainModel = forceInterAd ?: key.getModelByKey().second?.interAd
        val mainAd = mainModel
        mainAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                }

                override fun onAdDismissedFullScreenContent() {
                    interAdShowing = false
                    LastAdShownTime = System.currentTimeMillis()
                    key.onAdClosed(true)
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    key.onAdClosed()
                }

                override fun onAdImpression() {
                }

                override fun onAdShowedFullScreenContent() {
                    key.onAdShown()
                }
            }
        }
    }

    private fun String.onAdClosed(adShown: Boolean = false) {
        listener[this]?.onAdClosed(adShown)
        listener[this] = null
    }

    private fun String.onAdShown() {
        listener[this]?.onAdShown()
    }

    fun toast(msg: String) {
        if (isDebugMode) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun setInterAds(interstitialModels: List<InterstitialModel>) {
        logError("Set Inter Ad Called Size:${interstitialModels.size}")
        interAdsList.update {
            interstitialModels.filter { it.isEnabled.toBoolean() }.map {
                it.toInterModel()
            }
        }
    }

}
