package com.purvesh.tadavitv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
        "netflix" to listOf("com.netflix.ninja")
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
                        startActivity(intent)
                        return@runOnUiThread
                    }
                }
                Toast.makeText(this@MainActivity, "App not installed. Opening Play Store...", Toast.LENGTH_LONG).show()
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packages[0]))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Install it from the Play Store", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val patchJs = """
        (function(){
          if (window.__tadaviPatched) return;
          window.__tadaviPatched = true;
          function keyOf(tile){
            var t = (tile.getAttribute('data-title') || '').toLowerCase();
            if (t.indexOf('youtube') > -1) return 'youtube';
            if (t.indexOf('prime') > -1) return 'prime';
            if (t.indexOf('hotstar') > -1) return 'hotstar';
            if (t.indexOf('sony') > -1) return 'sonyliv';
            if (t.indexOf('spotify') > -1) return 'spotify';
            if (t.indexOf('xiaomi') > -1) return 'xiaomitv';
            if (t.indexOf('netflix') > -1) return 'netflix';
            return null;
          }
          function go(el, e){
            var tile = (el && el.closest) ? el.closest('.app-tile') : null;
            if (!tile) return;
            var k = keyOf(tile);
            if (!k) return;
            e.preventDefault();
            e.stopImmediatePropagation();
            Android.launch(k);
          }
          window.addEventListener('keydown', function(e){
            if (e.key === 'Enter' || e.keyCode === 13 || e.keyCode === 23) {
              go(document.querySelector('.app-tile.tv-focused'), e);
            }
          }, true);
          window.addEventListener('click', function(e){ go(e.target, e); }, true);
        })();
    """.trimIndent()

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
            addJavascriptInterface(Bridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(patchJs, null)
                }
            }
            webChromeClient = WebChromeClient()
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        setContentView(webView)
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
