package com.brouken.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WebShellActivity : AppCompatActivity() {

    private var webView: WebView? = null

    private class JSBridge(private val activity: AppCompatActivity) {
        @JavascriptInterface
        fun playUrl(url: String?) {
            val u = url?.trim().orEmpty()
            if (u.isEmpty()) return
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                activity.startActivity(intent)
            } catch (_: Throwable) { }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 先检测 WebView 提供方（部分设备无 WebView，会直接崩）
        if (Build.VERSION.SDK_INT >= 24) {
            val pkg = try { WebView.getCurrentWebViewPackage() } catch (_: Throwable) { null }
            if (pkg == null) {
                showNoWebViewScreen("此设备未安装或禁用了 Android System WebView，无法显示内置网页。")
                return
            }
        }

        // 2) 再尝试构造 WebView（个别机型会在构造时报错）
        val wv = try { WebView(this) } catch (t: Throwable) {
            showNoWebViewScreen("WebView 初始化失败：${t.javaClass.simpleName}")
            return
        }
        webView = wv
        setContentView(wv)

        WebView.setWebContentsDebuggingEnabled(true)

        val s: WebSettings = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        wv.webChromeClient = WebChromeClient()
        wv.addJavascriptInterface(JSBridge(this), "Android")

        // 3) 加载本地 assets/index.html
        wv.loadUrl("file:///android_asset/index.html")
    }

    private fun showNoWebViewScreen(message: String) {
        // 用一个极简原生界面替代“直接退出”，给出说明与安装按钮
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val tv = TextView(this).apply {
            text = message
            textSize = 16f
        }
        val btnInstall = Button(this).apply {
            text = "安装/启用 Android System WebView"
            setOnClickListener {
                // 优先尝试市场协议，失败则跳网页
                val pkg = "com.google.android.webview"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                } catch (_: ActivityNotFoundException) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                    } catch (e: Exception) {
                        Toast.makeText(this@WebShellActivity, "无法打开应用市场或浏览器", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        val btnClose = Button(this).apply {
            text = "关闭"
            setOnClickListener { finish() }
        }
        root.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 24
        })
        root.addView(btnInstall, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 16
        })
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
