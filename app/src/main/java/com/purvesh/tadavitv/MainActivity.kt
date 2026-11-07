package com.purvesh.tadavitv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var lastBack = 0L

    private val apps = mapOf(
        "youtube" to listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
        "prime" to listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
        "hotstar" to listOf("in.startv.hotstar.dplus.tv", "in.startv.hotstar"),
        "sonyliv" to listOf("com.sonyliv"),
        "spotify" to listOf("com.spotify.tv.android", "com.spotify.music"),
        "xiaomitv" to listOf("com.xiaomi.mitv.tvplus", "com.mitv.tvhome.atv", "com.mitv.livetv"),
        "netflix" to listOf("com.netflix.ninja"),
        "playstore" to listOf("com.android.vending")
    )

    inner class Bridge {
        @JavascriptInterface
        fun launch(key: String) {
            runOnUiThread {
                val packages = apps[key] ?: return@runOnUiThread
                for (pkg in packages) {
                    val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                        ?: packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
