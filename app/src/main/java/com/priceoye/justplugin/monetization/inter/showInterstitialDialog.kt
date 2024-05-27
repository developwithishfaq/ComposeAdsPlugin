package com.priceoye.justplugin.monetization.inter

import com.priceoye.justplugin.monetization.app_open.AppOpenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

val showInterstitialDialog = MutableStateFlow(false)

var interAdShowing = false

var LastAdShownTime: Long = 0
fun Int?.toBoolean(check: Boolean = true): Boolean {
    return this?.let {
        it == 1
    } ?: run {
        check
    }
}


fun InterstitialModel.toInterModel() = MyInterManager.InterAdModel(
    adKey,
    null,
    canRequestAd = true,
    counter = counterPatterns.getOrNull(0) ?: 0,
    adId,
    null
)

fun AppOpenPlanAd.toSimpleModel() = AppOpenManager.OpenAppAdModel(
    adKey = adKey,
    openAd = null,
    canRequestAd = true,
    adId = adId,
    listener = null
)


@Serializable
data class AppOpenPlanAd(
    val isEnabled: Int? = null,
    val adKey: String = "",
    val adId: String = "",
)

@Serializable
data class InterstitialModel(
    val isEnabled: Int? = null,
    val adKey: String = "",
    val adId: String = "",
    val counterPatterns: List<Int> = emptyList(),
    val preLoadBeforeReached: Int? = null,
    val showInstantIfNotLoaded: Int? = null,
    val dialogLoadingTime: Long = 1000,
    val instantDialogMaxTime: Long = 1000,
    val canRequestNewAd: Int? = null,
    val isBackAd: Int? = null,
) {
    fun isCounterAd(): Boolean {
        return true
    }
}