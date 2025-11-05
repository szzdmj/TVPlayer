package com.brouken.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WebShellActivity : AppCompatActivity() {

    private var webView: WebView? = null
    @Volatile private var playEnabled: Boolean = false

    private fun isPlayableUrl(u: String?): Boolean {
        if (u.isNullOrBlank()) return false
        val s = u.trim().lowercase()
        return s.startsWith("http://") || s.startsWith("https://") ||
               s.startsWith("rtsp://") || s.startsWith("rtmp://") ||
               s.startsWith("file://") || s.startsWith("content://")
    }

    private inner class JSBridge {
        @JavascriptInterface fun enablePlay(enabled: Boolean) {
            playEnabled = enabled
            runOnUiThread {
                Toast.makeText(this@WebShellActivity, if (enabled) "已解锁播放" else "已上锁播放", Toast.LENGTH_SHORT).show()
            }
        }
        @JavascriptInterface fun isPlayEnabled(): Boolean = playEnabled

        @JavascriptInterface fun playUrl(url: String?) {
            val u = url?.trim().orEmpty()
            if (!playEnabled) {
                runOnUiThread {
                    Toast.makeText(this@WebShellActivity, "已拦截播放（未解锁）", Toast.LENGTH_LONG).show()
                }
                return
            }
            if (!isPlayableUrl(u)) {
                runOnUiThread {
                    Toast.makeText(this@WebShellActivity, "已拦截播放：无效链接或不支持的协议", Toast.LENGTH_LONG).show()
                }
                return
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
            } catch (_: Throwable) {
                runOnUiThread { Toast.makeText(this@WebShellActivity, "无法打开此链接", Toast.LENGTH_LONG).show() }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 24) {
            val pkg = try { WebView.getCurrentWebViewPackage() } catch (_: Throwable) { null }
            if (pkg == null) {
                showNoWebViewScreen("此设备未安装或禁用了 Android System WebView，无法显示内置网页。")
                return
            }
        }

        val wv = try { WebView(this) } catch (t: Throwable) {
            showNoWebViewScreen("WebView 初始化失败：${t.javaClass.simpleName}")
            return
        }
        webView = wv
        setContentView(wv)

        WebView.setWebContentsDebuggingEnabled(true)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false) // 禁止多窗口
        }

        // 强拦截：页面内任何导航都不离开本地 index.html
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return true // 全部拦截
            }
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return true // 旧 API 也拦截
            }
        }
        // 禁止新窗口
        wv.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                return false
            }
        }

        // 注入 JS 桥（默认“上锁播放”）
        wv.addJavascriptInterface(JSBridge(), "Android")

        wv.loadUrl("file:///android_asset/index.html")
    }

    private fun showNoWebViewScreen(message: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 48)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val tv = TextView(this).apply { text = message; textSize = 16f }
        val btnInstall = Button(this).apply {
            text = "安装/启用 Android System WebView"
            setOnClickListener {
                val pkg = "com.google.android.webview"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                } catch (_: ActivityNotFoundException) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                    } catch (_: Exception) {
                        Toast.makeText(this@WebShellActivity, "无法打开应用市场或浏览器", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        val btnClose = Button(this).apply { text = "关闭"; setOnClickListener { finish() } }
        root.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 })
        root.addView(btnInstall, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 })
        root.addView(btnClose, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.let { w ->
            (w.parent as? ViewGroup)?.removeView(w)
            w.destroy()
        }
        webView = null
    }
}
