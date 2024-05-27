package com.priceoye.justplugin.monetization.native_ads.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.priceoye.justplugin.R


fun String.getAdLayout(context: Context): View {
    try {
        val resourceId = context.resources.getIdentifier(this, "layout", context.packageName)
        return LayoutInflater.from(context).inflate(resourceId, null, false)
    } catch (_: Exception) {
        return LayoutInflater.from(context).inflate(R.layout.small_native_ad, null, false)
    }
}