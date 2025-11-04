package com.brouken.player

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class WebShellActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private class JSBridge(private val activity: AppCompatActivity) {
        @JavascriptInterface
        fun playUrl(url: String?) {
            if (url.isNullOrBlank()) return
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
                activity.startActivity(intent)
            } catch (_: Throwable) {
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        WebView.setWebContentsDebuggingEnabled(true)

        val s: WebSettings = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(JSBridge(this), "Android")

        // 直接加载本地 assets 下的 index.html
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        super.onDestroy()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}
