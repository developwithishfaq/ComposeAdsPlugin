package com.priceoye.justplugin.monetization.bannerAds

import com.priceoye.justplugin.monetization.commons.BannerAdModel
import com.google.android.gms.ads.AdSize


fun BannerAdModel.getSize(): AdSize {
    val size = this.adType
    val sizeModel = when (size.uppercase()) {
        "BANNER" -> {
            AdSize.BANNER
        }

        "FULL_BANNER" -> {
            AdSize.FULL_BANNER
        }

        "LARGE_BANNER" -> {
            AdSize.LARGE_BANNER
        }

        "MEDIUM_RECTANGLE" -> {
            AdSize.MEDIUM_RECTANGLE
        }

        else -> {
            AdSize.MEDIUM_RECTANGLE
        }
    }
    return sizeModel
}