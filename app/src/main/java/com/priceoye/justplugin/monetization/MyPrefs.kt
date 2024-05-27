package com.priceoye.justplugin.monetization

import android.content.Context
import com.priceoye.justplugin.monetization.Constants.isDebugMode

class MyPrefs(
    context: Context
) {
    private val prefs = context.getSharedPreferences("Main", Context.MODE_PRIVATE)
    private val prefsEdit = prefs.edit()

    var adsJson: String
        get() = prefs.getString("adsJson", "") ?: ""
        set(value) {
            prefsEdit.putString("adsJson", value).apply()
        }

    var appPurchased: Boolean
        get() = if (isDebugMode) {
            false
        } else {
            prefs.getBoolean(
                "appPurchased",
                false
            )
        }
        set(value) = prefsEdit.putBoolean("appPurchased", value).apply()


}