package com.priceoye.justplugin.monetization.app_open

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.priceoye.justplugin.monetization.Constants.logError


class AppOpenCommunicator(
    private val appOpenManager: AppOpenManager
) : DefaultLifecycleObserver {

    private var appClass: BaseApp? = null

    fun initAppClass(appClass: BaseApp) {
        this.appClass = appClass
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (_: Exception) {
        }
    }

    private val key = "Normal"

    fun canShowAd(): Boolean {
        return appClass?.activityRef?.let {
            true
        } ?: false
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appClass?.activityRef?.let { activity ->
            if (canShowAd().not()) {
                logError(
                    "Cannot SHow Ad On This Activity:${
                        activity.localClassName.substringBefore(
                            "."
                        )
                    }", error = true
                )
                return
            }
            appInPauseState = false
            appOpenManager.showAd(
                context = activity,
                key = key,
                forceAppOpenAd = null,
                onFree = {

                },
                loadNewIfShown = true
            )
        }
    }

}