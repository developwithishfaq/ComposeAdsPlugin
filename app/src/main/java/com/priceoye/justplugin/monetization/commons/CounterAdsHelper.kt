package com.priceoye.justplugin.monetization.commons

import android.content.Context
import com.priceoye.justplugin.monetization.MyPrefs


object SimpleCounterHelper {
    fun CounterAdsHelper.incrementCounter(key: String) {
        setCounterValue(key, getCurrentCounter(key) + 1)
    }

    fun CounterAdsHelper.resetCounter(key: String) {
        setCounterValue(key, 0)
    }

    private fun CounterAdsHelper.setCounterValue(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun CounterAdsHelper.getCurrentCounter(key: String): Int {
        return prefs.getInt(key, 0)
    }

}

class CounterAdsHelper(
    context: Context,
    private val mPrefs: MyPrefs
) {
    fun reset() {

    }

    val prefs = context.getSharedPreferences("Main", Context.MODE_PRIVATE)

    /*
    fun setPatternValues(model: InterstitialModel, value: Int) {
        putValues(model.adKey + "_value", value)
    }

    fun getPatternValues(model: InterstitialModel): Int {
        return getValues(model.adKey + "_value", 0)
    }

    fun getPatternIndex(model: InterstitialModel): Int {
        return getValues(model.adKey + "_index", 0)
    }

    fun setPatternIndex(model: InterstitialModel, value: Int) {
        putValues(model.adKey + "_index", value)
    }

    private fun putValues(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    private fun getValues(key: String, default: Int = 0): Int {
        return prefs.getInt(key, default)
    }


    fun reset() {
        mPrefs.getAllAdsPlan()?.interstitialAds?.ads?.forEach {
            setPatternValues(it, 0)
            setPatternIndex(it, 0)
        }
    }

    fun incrementPatternValue(model: InterstitialModel) {
        val patternIndex = getPatternIndex(model)
        val peakValue = model.counterPatterns[patternIndex]
        val currentValue = getPatternValues(model)
        Log.d("counter", "incrementPatternValue: $currentValue  $peakValue ")
        if (currentValue == peakValue) {
            incrementPatternIndex(model)
        } else {
            setPatternValues(model, getPatternValues(model) + 1)
        }
    }

    fun incrementPatternIndex(model: InterstitialModel) {
        val patternMaxIndex = model.counterPatterns.size
        val patternIndex = getPatternIndex(model)
        Log.d("counter", "incrementPatternIndex: $patternIndex $patternMaxIndex ")
        if (patternIndex >= patternMaxIndex - 1) {
            setPatternIndex(model, 0)
            setPatternValues(model, 0)
        } else {
            setPatternIndex(model, getPatternIndex(model) + 1)
            setPatternValues(model, 0)
        }
    }

    fun onAdShown(interAdType: InterstitialModel) {
        incrementPatternValue(interAdType)
    }
*/
}