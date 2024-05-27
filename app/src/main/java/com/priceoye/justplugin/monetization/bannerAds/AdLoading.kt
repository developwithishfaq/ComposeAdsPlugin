package com.priceoye.justplugin.monetization.bannerAds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.priceoye.justplugin.R
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp

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