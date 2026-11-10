package com.purvesh.tadavitv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // Handle Android TV Native App Intents (intent://)
                if (url.startsWith("intent://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            val pkg = intent.package_
                            // Launch installed TV app directly by package name
                            if (pkg != null) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                    return true
                                }
                            }
                            // Fallback to general intent resolution
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                            // If app is missing on TV, redirect to Play Store
                            if (pkg != null) {
                                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                                startActivity(marketIntent)
                                return true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }
                return false
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // Keep app active on home screen when back is pressed
        }
    }
}
