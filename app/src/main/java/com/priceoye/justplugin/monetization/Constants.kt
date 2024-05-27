package com.priceoye.justplugin.monetization

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.datatransport.BuildConfig
import java.io.File
import java.net.URLEncoder


object Constants {
    fun String.toIntOrZero(check: Int = 0) = this.toIntOrNull() ?: check
    fun Char.toIntOrZero(check: Int = 0) = this.toString().toIntOrZero()

    val isDebugMode = BuildConfig.DEBUG

    fun logError(msg: String, key: String = "", error: Boolean = false) {
        if (error) {
            Log.e("adsPlugin", "ADS:$key-$msg")
        } else {
            Log.d("adsPlugin", "ADS:$key-$msg")
        }
    }

    fun String.adsToast(context: Context) {
        if (isDebugMode) {
            Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
        }
    }

    fun String.isYoutubeLink(): Boolean {
        return this.contains("youtube.com", true) || this.contains(
            "youtube.", true
        ) || this.contains("yout.com", true) || this.contains(
            "yout.", true
        ) || this.contains("youtu.com", true) || this.contains(
            "youtu.", true
        ) || this.contains("youtube", true)
    }

    fun String.isInstagramLink(): Boolean {
        return this.contains("www.instagram.com", true)
    }

    fun String.isGoodLink(): Boolean {
        return this.length > 6
    }

    fun File.sizeStr(context: Context): String {
        return Formatter.formatFileSize(context, length())
    }

    private fun Long.sizeStr(context: Context): String {
        return Formatter.formatFileSize(context, this)
    }

    fun String?.goodSize(context: Context): String {
        return if (this?.toLongOrNull() == null) {
            this ?: "0 B"
        } else {
            this.toLong().sizeStr(context)
        }
    }


    fun String.getErrorMessage(): String {
        return when {
            this.containsAny("Unable to resolve host", "timeout", "timed out") -> NO_INTENET
            else -> this
        }
    }


    fun String.containsAny(vararg strings: String): Boolean {
        strings.forEach {
            if (this.contains(it, true)) {
                return true
            }
        }
        return false
    }


    const val NO_INTENET = "Could not load video, Please check your internet connection"
}