package com.priceoye.justplugin.monetization.commons

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object CommonAds {


    fun Context.getStringRes(@StringRes res: Int): String {
        return ContextCompat.getString(this, res)
    }

    @Composable
    fun getContext(): Context {
        return LocalContext.current
    }

    @Composable
    fun getContextAsActivity(): Activity? {
        return LocalContext.current.getActivity()
    }

    fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }
}