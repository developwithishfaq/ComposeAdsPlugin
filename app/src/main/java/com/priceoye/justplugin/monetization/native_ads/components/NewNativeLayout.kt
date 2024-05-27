package com.priceoye.justplugin.monetization.native_ads.components

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.priceoye.justplugin.monetization.commons.getNativeModelByKey
import com.priceoye.justplugin.monetization.native_ads.NativeNewAddsViewModel
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.priceoye.justplugin.R
import com.priceoye.justplugin.monetization.Constants.logError
import com.priceoye.justplugin.monetization.MyPrefs
import com.priceoye.justplugin.monetization.commons.CommonAds.getContextAsActivity
import com.priceoye.justplugin.monetization.inter.toBoolean
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp
import org.koin.androidx.compose.koinViewModel


@Composable
fun NewNativeLayout(
    key: String,
    canBeDestroyed: Boolean = true,
    modifier: Modifier = Modifier,
    inHorizontalPager: Boolean = false,
    viewModel: NativeNewAddsViewModel = koinViewModel()
) {
    Column {


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
        val adModel = key.getNativeModelByKey(viewModel.getPref())
        val context = getContextAsActivity()
        val hideLayout by rememberSaveable {
            mutableStateOf(false)
        }
        var adFailed by remember {
            mutableStateOf(false)
        }
        var nativeAdAdmob by remember {
            mutableStateOf<NativeAd?>(null)
        }
        LaunchedEffect(Unit) {
            nativeAdAdmob = viewModel.getNativeAd(key)
        }
        LaunchedEffect(key1 = Unit) {
            logError("NewNativeLayout Called Ad Model: $adModel")
        }

        LaunchedEffect(key1 = hideLayout.not() && adModel != null && adModel.adEnabled.toBoolean() && adFailed.not()) {
            logError("NewNativeLayout Called : $key, enabled: ${adModel?.adEnabled.toBoolean()} ,isAdFailed:$adFailed, Hide Layout:$hideLayout")
        }
        if (hideLayout.not() && adModel != null && adModel.adEnabled.toBoolean() && adFailed.not()) {
            Card(
                modifier = modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.adBg)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(10.sdp),
            ) {
                nativeAdAdmob?.let { nativeAd ->
                    LaunchedEffect(key1 = Unit) {
                        logError("Native Admob not null for $key")
                    }
                    AndroidView(
                        modifier = Modifier.padding(
                            start = 2.dp,
                            top = 2.dp,
                            end = 2.dp,
                            bottom = 2.dp
                        ),
                        factory = {
                            adModel.adType.getAdLayout(it)
                        },
                    ) {
                        context?.let { it1 -> populateAd(it1, viewModel.getPref(), it, nativeAd) }
                    }
                } ?: run {
                    LaunchedEffect(Unit) {
                        logError("nativeAdAdmob for($key) null else condition, call show ad")
                        context?.let {
                            viewModel.showAd(
                                key = key,
                                context = it,
                                object : NativeNewAddsViewModel.NewNativeAdListener {
                                    override fun onLoaded(nativeAd: NativeAd) {
                                        if (isVisible) {
                                            logError("Native Ad Populated ($key)")
                                            nativeAdAdmob = nativeAd
                                        }
                                        if (isVisible && inHorizontalPager.not()) {
                                            viewModel.destroyAd(key)
                                        }
                                    }

                                    override fun onFailure() {
                                        logError("Native Ad Failed (key=$key)")
                                    }
                                },
                                canBeDestroyed = canBeDestroyed
                            )
                        }
                    }
                    AdLoading(adModel.height)
                }
            }
        } else {
//            Text(text = "Hahahahhahahah ${hideLayout.not()}, ${adModel != null}, ${adModel?.adEnabled.toBoolean()}, ${adFailed.not()}")
        }
    }
}


@Composable
fun AdLoading(height: Int = 80) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.sdp)
            .background(colorResource(id = R.color.greyE4), RoundedCornerShape(12.sdp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "advertisement_area",
                modifier = Modifier
                    .align(Alignment.Center),
                fontSize = 9.ssp
            )
        }
    }
}


fun populateAd(context: Context, prefs: MyPrefs, view: View, nativeAd: NativeAd) {
    val nativeAdView: NativeAdView = view.findViewById(R.id.nativeAd)

    val adIcon: ImageView = view.findViewById(R.id.adIcon)
    val adHeadLine: TextView = view.findViewById(R.id.adHeadline)
    val adBody: TextView = view.findViewById(R.id.adBody)
    val adCtaBtn: TextView = view.findViewById(R.id.adCtaBtn)

    val mMedia = view.findViewById<MediaView>(R.id.mediaView)
    nativeAdView.mediaView = mMedia
    try {
        nativeAdView.mediaView?.let { adMedia ->
            adMedia.makeGone(nativeAd.mediaContent == null)
            mMedia.makeGone(nativeAd.mediaContent == null)
            if (nativeAd.mediaContent != null) {
                adMedia.mediaContent = nativeAd.mediaContent
            }
        } ?: run {
            nativeAdView.mediaView?.makeGone()
            mMedia?.makeGone()
        }
    } catch (_: Exception) {
        nativeAdView.mediaView?.makeGone()
        mMedia?.makeGone()
    }
    val mIconView = nativeAdView.findViewById<ImageView>(R.id.adIcon)
    nativeAdView.iconView = mIconView
    nativeAdView.iconView?.let {
        nativeAd.icon.let { icon ->
            nativeAdView.mediaView?.makeGone(icon == null)
            if (icon != null) {
                (it as ImageView).setImageDrawable(icon.drawable)
            }
        }
    } ?: run {
        mIconView.makeGone()
    }

    nativeAdView.callToActionView = adCtaBtn
    nativeAdView.bodyView = adBody
    nativeAdView.iconView = adIcon
    nativeAdView.headlineView = adHeadLine

    if (nativeAd.headline.isNullOrEmpty()) {
        adHeadLine.visibility = View.GONE
    } else {
        adHeadLine.visibility = View.VISIBLE
        adHeadLine.text = nativeAd.headline
    }

    if (nativeAd.body.isNullOrEmpty()) {
        adBody.visibility = View.GONE
    } else {
        adBody.visibility = View.VISIBLE
        adBody.text = nativeAd.body
    }
    if (nativeAd.icon == null) {
        adIcon.visibility = View.GONE
    } else {
        adIcon.setImageDrawable(nativeAd.icon!!.drawable)
        adIcon.visibility = View.VISIBLE
    }
    nativeAd.callToAction?.let { btn ->
        adCtaBtn.text = btn
    }
    nativeAdView.setNativeAd(nativeAd)
}

@Composable
fun AdLoadingNative() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 2.sdp, vertical = 2.sdp
            ),
        shape = RoundedCornerShape(10)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.sdp), contentAlignment = Alignment.Center
        ) {
            Text(text = "Ad Is Loading")
        }
    }
}

fun View.makeGone(check: Boolean = true) {
    visibility = if (check) {
        View.GONE
    } else {
        View.VISIBLE
    }
}

fun View.makeVisible(check: Boolean = true) {
    visibility = if (check) {
        View.VISIBLE
    } else {
        View.GONE
    }
}