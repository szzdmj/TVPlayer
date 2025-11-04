package com.brouken.player

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
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

        // 设备未安装或禁用 WebView 时，安全退出，避免崩溃
        if (Build.VERSION.SDK_INT >= 24) {
            val pkg = try { WebView.getCurrentWebViewPackage() } catch (_: Throwable) { null }
            if (pkg == null) {
                Toast.makeText(this, "此设备未安装或禁用了 WebView，无法打开网页壳", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        val wv = try { WebView(this) } catch (t: Throwable) {
            Toast.makeText(this, "WebView 初始化失败：${t.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            finish()
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

        // 加载本地 assets 下的 index.html（合入 tvweb 的稳健外挂、再加复制播放）
        wv.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.let { w ->
            (w.parent as? android.view.ViewGroup)?.removeView(w)
            w.destroy()
        }
        webView = null
    }
}
