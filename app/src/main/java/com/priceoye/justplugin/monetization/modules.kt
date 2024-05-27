package com.priceoye.justplugin.monetization

import com.priceoye.justplugin.monetization.app_open.AppOpenManager
import com.priceoye.justplugin.monetization.bannerAds.BannerAdViewModel
import com.priceoye.justplugin.monetization.commons.AdsRules
import com.priceoye.justplugin.monetization.commons.CounterAdsHelper
import com.priceoye.justplugin.monetization.consent.GoogleConsent
import com.priceoye.justplugin.monetization.inter.InterstitialViewModel
import com.priceoye.justplugin.monetization.inter.MyInterManager
import com.priceoye.justplugin.monetization.native_ads.NativeNewAddsViewModel
import com.priceoye.justplugin.monetization.native_ads.NewNativeAdsManager
import com.priceoye.justplugin.monetization.remote_config.RemoteConfigController
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    single {
        NewNativeAdsManager(get(), get(), get(), get())
    }
    single {
        MyInterManager(get(), get(), get(), get(), get())
    }
    factory {
        CounterAdsHelper(get(), get())
    }
    factory {
        InternetController(get())
    }
    single {
        AdsRules(get(), get(), get(), get())
    }
    single { AppOpenManager(get(), get(), get()) }
    single { RemoteConfigController(get(), get(), get()) }

    viewModel {
        NativeNewAddsViewModel(get(), get(), get())
    }
    viewModel {
        InterstitialViewModel(get(), get(), get())
    }
    viewModel {
        BannerAdViewModel(get(), get(), get())
    }

    single { GoogleConsent(get(), get()) }

    single<MyPrefs> {
        MyPrefs(get())
    }

}