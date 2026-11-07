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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// PUT YOUR FREE TMDB API KEY HERE (themoviedb.org -> Settings -> API)
const val TMDB_KEY = "6b49683e9e12ffc3730374cfde203e34"

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

    private val feeds = listOf(
        "INDIA" to "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en",
        "MAHARASHTRA" to "https://news.google.com/rss/search?q=Maharashtra&hl=en-IN&gl=IN&ceid=IN:en",
        "PUNE" to "https://news.google.com/rss/search?q=Pune&hl=en-IN&gl=IN&ceid=IN:en",
        "JALGAON" to "https://news.google.com/rss/search?q=Jalgaon&hl=en-IN&gl=IN&ceid=IN:en"
    )

    private fun http(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun clean(s: String) = s.replace("&amp;", "&").replace("&quot;", "\"")
        .replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")

    private fun push(fn: String, data: String) {
        runOnUiThread { webView.evaluateJavascript("window.$fn(${JSONObject.quote(data)})", null) }
    }

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
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packages[0]))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Install it from the Play Store", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun openSettings(key: String) {
            runOnUiThread {
                val action = when (key) {
                    "picture" -> Settings.ACTION_DISPLAY_SETTINGS
                    "sound" -> Settings.ACTION_SOUND_SETTINGS
                    "wifi" -> Settings.ACTION_WIFI_SETTINGS
                    "network" -> Settings.ACTION_WIRELESS_SETTINGS
                    "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                    "apps" -> Settings.ACTION_APPLICATION_SETTINGS
                    "about" -> Settings.ACTION_DEVICE_INFO_SETTINGS
                    "developer" -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                    else -> Settings.ACTION_SETTINGS
                }
                try {
                    startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e2: Exception) {
                        Toast.makeText(this@MainActivity, "Settings not available", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        @JavascriptInterface
        fun fetchNews() {
            Thread {
                val out = JSONArray()
                var err = ""
                for ((label, url) in feeds) {
                    try {
                        val xml = http(url)
                        val items = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL).findAll(xml).take(6)
                        for (m in items) {
                            val s = m.groupValues[1]
                            fun tag(t: String) = Regex("<$t[^>]*>(.*?)</$t>", RegexOption.DOT_MATCHES_ALL)
                                .find(s)?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim() ?: ""
                            out.put(JSONObject().put("region", label).put("title", clean(tag("title")))
                                .put("src", clean(tag("source"))).put("date", tag("pubDate")))
                        }
                    } catch (e: Exception) { err = e.toString() }
                }
                push("onNews", if (out.length() > 0) out.toString() else JSONObject().put("error", err).toString())
            }.start()
        }

        @JavascriptInterface
        fun fetchMovies() {
            Thread {
                if (TMDB_KEY.startsWith("PUT")) { push("onMovies", "{\"nokey\":true}"); return@Thread }
                try {
                    val base = "https://api.themoviedb.org/3"
                    val t = JSONObject(http("$base/trending/movie/day?api_key=$TMDB_KEY&language=en-IN"))
                    val h = JSONObject(http("$base/discover/movie?api_key=$TMDB_KEY&with_original_language=hi" +
                        "&region=IN&sort_by=popularity.desc&primary_release_date.gte=2025-01-01"))
                    val o = JSONObject().put("trending", t.optJSONArray("results")).put("hindi", h.optJSONArray("results"))
                    push("onMovies", o.toString())
                } catch (e: Exception) { push("onMovies", JSONObject().put("error", e.toString()).toString()) }
            }.start()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        webView = WebView(this).apply {
            setBackgroundColor(0xFF05070D.toInt())
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            addJavascriptInterface(Bridge(), "Android")
            webViewClient = WebViewClient()
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
        if (now - lastBack < 1500) { super.onBackPressed(); return }
        lastBack = now
        webView.evaluateJavascript(
            "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',keyCode:27,bubbles:true}));", null)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
