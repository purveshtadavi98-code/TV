package com.purvesh.tadavitv

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var lastBack = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = WebView(this).apply {
            setBackgroundColor(0xFF02040A.toInt())
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        setContentView(webView)

        // Bundled dashboard: app/src/main/assets/index.html
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    // BACK: first press sends ESC to the page (closes popups); press twice within 1.5s to exit.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBack < 1500) {
            super.onBackPressed()
            return
        }
        lastBack = now
        webView.evaluateJavascript(
            "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',keyCode:27,bubbles:true}));",
            null
        )
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
