package com.priceoye.justplugin.monetization.bannerAds

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.priceoye.justplugin.monetization.commons.getBannerModelByKey
import com.priceoye.justplugin.monetization.inter.toBoolean
import com.google.android.gms.ads.AdView
import org.koin.androidx.compose.koinViewModel

fun Context.getActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.getActivity()
        else -> null
    }
}


@Composable
fun BannerAdView(
    key: String,
    modifier: Modifier = Modifier,
    canBeDestroyed: Boolean = true,
    inHorizontalPager: Boolean = true,
    viewModel: BannerAdViewModel = koinViewModel(),
): BannerAdViewModel {
    val context = LocalContext.current
    val activity = context.getActivity()


    var isVisible by remember {
        mutableStateOf(true)
    }
    DisposableEffect(Unit) {
        isVisible = true
        Log.d("cvv", "NewNativeLayout: expose")
        onDispose {
            isVisible = false
            Log.d("cvv", "NewNativeLayout: Disposed")
        }
    }

    val adModel = key.getBannerModelByKey(viewModel.getPref())

    val hideLayout by rememberSaveable {
        mutableStateOf(false)
    }
    var adFailed by remember {
        mutableStateOf(false)
    }
    var nativeAdAdmob by remember {
        mutableStateOf<AdView?>(null)
    }
    LaunchedEffect(Unit) {
        nativeAdAdmob = viewModel.getBannerAd(key)
    }
    if (hideLayout.not() && adModel != null && adModel.adEnabled.toBoolean() && adFailed.not()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            nativeAdAdmob?.let { nativeAd ->
                AndroidView(modifier = Modifier, factory = {
                    try {
                        nativeAd
                    } catch (_: Exception) {
                        View(it)
                    }
                })
            } ?: run {
                LaunchedEffect(Unit) {
                    activity?.let {
                        viewModel.showAd(
                            key = key,
                            context = it,
                            onFailure = {
                                adFailed = true
                            },
                            onLoaded = {
                                if (isVisible) {
                                    nativeAdAdmob = it
                                }
                                if (isVisible && inHorizontalPager.not()) {
                                    viewModel.destroyBannerAd(key)
                                }
                            }, canBeDestroyed = canBeDestroyed
                        )
                    }
                }
                AdLoading(adModel.height)
            }
        }
    }
    return viewModel
}

/*
@Composable
fun BannerAdView(
    key: String,
    modifier: Modifier = Modifier,
    viewModel: BannerAdViewModel = koinViewModel(),
): BannerAdViewModel {
    val context = LocalContext.current
    val activity = context.getActivity()
    val adModel = key.getBannerModelByKey(viewModel.getPref())
    val adState by viewModel.adViewState.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    val isAdAvailable = adState.firstOrNull {
        it.first == adModel?.adKey && it.second != null
    }
    val hideLayout by viewModel.hideLayout.collectAsState(initial = false)

    if (hideLayout.not() && adModel != null) {
        isAdAvailable?.let { adPair ->
            Column(
                modifier = modifier.fillMaxWidth()
            ) {
                adPair.second?.let { adView ->
                    AndroidView(modifier = Modifier.fillMaxWidth(), factory = {
                        try {
                            adView
                        } catch (_: Exception) {
                            View(it)
                        }
                    })
                }
            }
        } ?: run {
            AdLoading(height = adModel.height ?: 80)
            LaunchedEffect(key1 = key) {
                activity?.let { viewModel.loadNewAd(key, it) }
            }
        }
    }
    return viewModel
}*/