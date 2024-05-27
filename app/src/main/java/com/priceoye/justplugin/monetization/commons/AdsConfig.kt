package com.priceoye.justplugin.monetization.commons

import android.content.Context
import com.priceoye.justplugin.monetization.inter.AppOpenPlanAd
import com.priceoye.justplugin.monetization.inter.InterstitialModel
import com.priceoye.justplugin.monetization.inter.toBoolean
import com.google.errorprone.annotations.Keep
import com.priceoye.justplugin.R
import com.priceoye.justplugin.monetization.Constants.isDebugMode
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.app_open.BaseApp
import com.priceoye.justplugin.monetization.app_open.No_Ads
import com.priceoye.justplugin.monetization.remote_config.isRemoteConfigsAssigned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


enum class UploadArea {
    PlayConsole, AppGallery
}

val uploadArea = UploadArea.PlayConsole

private val interBlockList = mutableListOf<String>()
private val nativeBlockList = mutableListOf<String>()
private val bannerBlockList = mutableListOf<String>()

fun clearBlockLists() {
    interBlockList.clear()
    nativeBlockList.clear()
    bannerBlockList.clear()
}

fun String.isInterBlocked(): Boolean {
    return interBlockList.contains(this)
}

fun String.isNativeBlocked(): Boolean {
    return nativeBlockList.contains(this)
}

fun String.isBannerBlocked(): Boolean {
    return bannerBlockList.contains(this)
}

fun String.blockInterstitial() {
    interBlockList.add(this)
}

fun String.blockNative() {
    nativeBlockList.add(this)
}

fun String.blockBanner() {
    bannerBlockList.add(this)
}

//private val allAdsList = MutableStateFlow<MainAdsPlan?>(null)

enum class AdTypes(val testIds: String) {
    Banner("ca-app-pub-3940256099942544/9214589741"), Interstitial("ca-app-pub-3940256099942544/1033173712"), Native(
        "ca-app-pub-3940256099942544/2247696110"
    )
}

private val allAdsFlow = MutableStateFlow<MainAdsPlan>(MainAdsPlan())

fun getAdsLiveFlow() = allAdsFlow
fun getAdsPlanSimple() = allAdsFlow.value


suspend fun getAllAdsFromJson(prefs: MyPrefs): String {
    val data = if (No_Ads && isDebugMode) {
        ""
    } else {
        if (isRemoteConfigsAssigned) {
            prefs.adsJson
        } else {
            try {
                if (prefs.adsJson.isBlank()) {
                    getAdsPlanFromJson() ?: ""
                } else {
                    if (isDebugMode) {
                        getAdsPlanFromJson() ?: ""
                    } else {
                        prefs.adsJson
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }
    }
    prefs.adsJson = data
    return data
}

fun MainAdsPlan.toDebug(): MainAdsPlan {
    val data = this
    val model =
        data.copy(interstitialAds = data.interstitialAds?.copy(ads = data.interstitialAds.ads.map {
            it.copy(
                adId = AdTypes.Interstitial.testIds
            )
        }), nativeAds = data.nativeAds?.copy(ads = data.nativeAds.ads.map {
            it.copy(
                adId = AdTypes.Native.testIds
            )
        }), bannerAds = data.bannerAds?.copy(ads = data.bannerAds.ads.map {
            it.copy(
                adId = AdTypes.Banner.testIds
            )
        }))
    return if (isDebugMode) {
        model
    } else {
        this
    }
}

inline fun <reified T> String.getModel() = try {
    Json.decodeFromString<T>(this)
} catch (_: Exception) {
    null
}

fun MyPrefs.getAllAdsPlan(): MainAdsPlan? = adsJson.getModel()

fun String.getNativeModelByKey(prefs: MyPrefs) =
    prefs.getAllAdsPlan()?.nativeAds?.ads?.firstOrNull { it.adKey == this }

fun getAdMainDefaults(prefs: MyPrefs) =
    prefs.getAllAdsPlan()?.adMainInfo

fun getNativeAdsList(prefs: MyPrefs) =
    prefs.getAllAdsPlan()?.nativeAds?.ads ?: emptyList()

fun getInterAdsList(prefs: MyPrefs) =
    prefs.getAllAdsPlan()?.interstitialAds?.ads ?: emptyList()


fun String.getBannerModelByKey(prefs: MyPrefs) =
    prefs.getAllAdsPlan()?.bannerAds?.ads?.firstOrNull { it.adKey == this }


@Serializable
data class MainAdsInfo(
    val ctaTextColor: String? = null,
    val ctaBgColor: String? = null,
    val adAttrTextColor: String? = null,
    val adAttrBgColor: String? = null,
)

//fun String.getColor(context: Context): Int {
//    return ContextCompat.getColor(context, Color.parseColor(this))
//}

@Keep
@Serializable
data class MainAdsPlan(
    val adMainInfo: MainAdsInfo? = null,
    val interstitialAds: InterAdsPlan? = null,
    val appOpenAds: AppOpenAdsPlan? = null,
    val nativeAds: NativeAdsPlan? = null,
    val bannerAds: BannerAdsPlan? = null,
    val rewardedAdPlans: RewardedAdPlans? = null
)

@Keep
@Serializable
data class NativeAdsPlan(
    val ads: List<NativeModel>
)

@Keep
@Serializable
data class RewardedAdPlans(
    val ads: List<RewardedAdModel>
)

@Keep
@Serializable
data class RewardedAdModel(
    val adId: String,
    val adKey: String,
    val adEnabled: Boolean,
)

@Keep
@Serializable
data class BannerAdsPlan(
    val ads: List<BannerAdModel>
)

@Keep
@Serializable
data class NativeModel(
    val adKey: String,
    val adEnabled: Int,
    val adId: String,
    val adType: String,
    val height: Int = 80,
    val isBackupAd: Int? = null,
)

@Keep
@Serializable
data class BannerAdModel(
    val adKey: String,
    val adEnabled: Int,
    val collapsible: Int? = null,
    val adId: String,
    val adType: String,
    val height: Int = 80
) {
    fun isCollapsible(): Boolean {
        return collapsible.toBoolean()
    }
}

@Keep
@Serializable
data class InterAdsPlan(
    val ads: List<InterstitialModel>
)

@Keep
@Serializable
data class AppOpenAdsPlan(
    val ads: List<AppOpenPlanAd>
)

suspend fun getAdsPlanFromJson(
) = readJsonFromRaw(BaseApp.appContext as Context, R.raw.news_ads_script)

/* return try {
     Json.decodeFromString(data ?: "")
 } catch (_: Exception) {
     null
 }
}
*/
suspend fun readJsonFromRaw(context: Context, resourceId: Int): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}/*
private val adsJson = """
    {
      "interstitialAds": {
        "ads": [
          {
            "isEnabled": 1,
            "adKey": "PublicPrivate",
            "adId": "ca-app-pub-3178740817814635/1076783457",
            "counterPatterns": [
              2
            ],
            "preLoadBeforeReached": 0,
            "showInstantIfNotLoaded": 0,
            "instantDialogMaxTime": 8000,
            "dialogLoadingTime": 1000,
            "canRequestNewAd": 0
          }
        ]
      },
      "nativeAds": {
        "ads": [
          {
            "adKey": "TestNative",
            "adEnabled": 1,
            "adId": "ca-app-pub-3940256099942544/2247696110",
            "adType": "small_right_side_native_ad",
            "height": 80
          }
        ]
      },
      "bannerAds": {
        "ads": [
          {
            "adKey": "MainBanner",
            "adEnabled": 1,
            "adId": "ca-app-pub-3178740817814635/8463663319",
            "adType": "LARGE_BANNER",
            "height": 70
          }
        ]
      }
    }*/
//""".trimIndent()