package com.priceoye.justplugin.monetization.app_open

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.priceoye.justplugin.monetization.Constants
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.InternetController
import com.priceoye.justplugin.monetization.consent.GoogleConsent
import com.priceoye.justplugin.monetization.inter.AppOpenPlanAd
import com.priceoye.justplugin.monetization.inter.LastAdShownTime
import com.priceoye.justplugin.monetization.inter.MyAdListener
import com.priceoye.justplugin.monetization.inter.interAdShowing
import com.priceoye.justplugin.monetization.inter.toBoolean
import com.priceoye.justplugin.monetization.inter.toSimpleModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


class AppOpenManager(
    private val context: Context,
    private val internetController: InternetController,
    private val consent: GoogleConsent
) {

    data class OpenAppAdModel(
        val adKey: String,
        val openAd: AppOpenAd? = null,
        val canRequestAd: Boolean = true,
        val adId: String = "",
        val listener: MyAdListener? = null
    )

    private val openAdsList = MutableStateFlow<List<OpenAppAdModel>>(emptyList())

    private val listener = HashMap<String, MyAdListener?>()


    private fun String.interAlreadyRequesting(): Boolean {
        return openAdsList.value.firstOrNull { it.canRequestAd.not() && this == it.adKey } != null
    }

    private fun String.setCanRequestAd(canRequestAd: Boolean) {
        val list = openAdsList.value.toMutableList()
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
            list.add(OpenAppAdModel(this, null, canRequestAd))
        }
        openAdsList.update {
            list
        }
    }


    private fun String.getModelByKey(): Pair<String, OpenAppAdModel?> {
        var adKey = this
        val availableAd = openAdsList.value.firstOrNull {
            it.openAd != null
        }
        val interModel = openAdsList.value.firstOrNull { this == it.adKey }
        val model = if (interModel != null) {
            if (interModel?.openAd != null) {
                adKey = interModel.adKey
                interModel
            } else if (availableAd?.openAd != null) {
                adKey = availableAd.adKey
                interModel?.copy(
                    openAd = availableAd.openAd
                )
            } else {
                interModel
            }
        } else {
            null
        }
        return Pair(adKey, model)
    }


    private fun setListener(key: String, myAdListener: MyAdListener) {
        listener[key] = myAdListener
    }

    fun showAd(
        context: Activity, key: String, forceAppOpenAd: AppOpenAd? = null,
        loadNewIfShown: Boolean = false,
        onFree: (Boolean) -> Unit,
    ) {
        if (forceAppOpenAd != null) {
            setListener(key, object : MyAdListener {
                override fun onAdClosed(adShown: Boolean) {
                    onFree.invoke(adShown)
                }
            })
            setAppOpenAdCallBacks(key, forceAppOpenAd)
            forceAppOpenAd.show(context)
        } else {
            val mainModel = key.getModelByKey()
            if (mainModel.second == null) {
                onFree.invoke(false)
                return
            }
            val adModel = mainModel.second
            if (adModel?.openAd != null) {
                setListener(key, object : MyAdListener {
                    override fun onAdClosed(adShown: Boolean) {
                        if (adShown) {
                            mainModel.first.setAppOpenAd(null, true)
                            if (loadNewIfShown) {
                                loadAppOpenAd(key)
                            }
                        }
                        onFree.invoke(adShown)
                    }
                })
                setAppOpenAdCallBacks(key, forceAppOpenAd)
                adModel.openAd.show(context)
            } else {
                onFree.invoke(false)
                loadAppOpenAd(key)
            }
        }
    }

    private fun setAppOpenAdCallBacks(key: String, forceAppOpenAd: AppOpenAd?) {
        val mainAd = forceAppOpenAd ?: key.getModelByKey().second?.openAd
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
        setAppOpenAd(null)
        listener[this] = null
    }

    private fun String.onAdShown() {
        listener[this]?.onAdShown()
    }


    private fun String.getAdAvailable(): AppOpenAd? =
        openAdsList.value.firstOrNull { this == it.adKey }?.openAd

    fun loadAppOpenAd(key: String) {
        key.getModelByKey().second ?: return
        val adAvailable = key.getAdAvailable()
        if (adAvailable != null) {
            logError("Ad Already Available for this key:$key")
            return
        }
        loadAppOpenAdCore(key, onLoaded = {
            key.setAppOpenAd(it)
        }, onFailed = {
            key.setAppOpenAd(null)
        })
    }


    fun loadAppOpenAdCore(
        key: String, onLoaded: (AppOpenAd) -> Unit, onFailed: () -> Unit
    ) {
        if (consent.canRequestAds.not()) {
            logError("Consent Not Allowed")
            onFailed.invoke()
            return
        }

        val model = key.getModelByKey()
        if (model.second == null) {
            logError("AppOpen : No Ad Model Found Againts (key=$key) ")
            onFailed.invoke()
            return
        }
        val adModel = model.second
        logError("loadAppOpenAdCore Model: $adModel, List Size:${openAdsList.value.size}")
        if (adModel == null) {
            onFailed.invoke()
            return
        }
        if (adModel.openAd != null) {
            onLoaded.invoke(adModel.openAd)
            return
        }
        if (internetController.isInternetConnected.not()) {
            logError("No Internet : $key")
            onFailed.invoke()
            return
        }
        if (key.interAlreadyRequesting()) {
            logError("$key is already requesting")
            return
        }
        key.setCanRequestAd(false)
        toast("AppOpen Ad Requested $key")
        logError("App Open Requested $key:${adModel.adId}")
        AppOpenAd.load(context,
            adModel.adId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    toast("App Open Loaded ($key)")
                    logError("App Open Loaded ($key)")
                    onLoaded.invoke(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    toast("App Open Failed to load ($key)")
                    logError("App Open Failed to load ($key)")
                    onFailed.invoke()
                }
            })
    }

    fun String.setAppOpenAd(interAdModel: AppOpenAd? = null, canRequestAd: Boolean = true) {
        val list = openAdsList.value.toMutableList()
        val index = list.indexOfFirst {
            it.adKey == this
        }
        if (index != -1) {
            val model = list[index]
            list.removeAt(index)
            list.add(
                model.copy(
                    openAd = interAdModel, canRequestAd = canRequestAd
                )
            )
        } else {
            list.add(OpenAppAdModel(this, interAdModel, canRequestAd))
        }
        openAdsList.update {
            list
        }
    }

    fun toast(msg: String) {
        if (Constants.isDebugMode) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun setAppOpenAds(adsList: List<AppOpenPlanAd>) {
        Constants.logError("Set Inter Ad Called Size:${adsList.size}")
        openAdsList.update {
            adsList.filter { it.isEnabled.toBoolean() }.map {
                it.toSimpleModel()
            }
        }
    }

}