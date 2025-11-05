package com.brouken.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WebInfoActivity : AppCompatActivity() {

    private lateinit var infoView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 根布局（纯原生 UI，避免任何跳转）
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "TVPlayer WebShell 环境信息"
            textSize = 18f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }

        val btnRefresh = Button(this).apply {
            text = "刷新"
            setOnClickListener { renderInfo() }
        }
        val btnCopy = Button(this).apply {
            text = "复制"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("env", infoView.text))
                Toast.makeText(this@WebInfoActivity, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        }
        val btnOpenWebShell = Button(this).apply {
            text = "打开 WebShell"
            setOnClickListener {
                try {
                    // 打开 WebShellActivity（不传任何播放 URL，避免触发播放器）
                    startActivity(Intent(this@WebInfoActivity, WebShellActivity::class.java))
                } catch (t: Throwable) {
                    Toast.makeText(this@WebInfoActivity, "无法打开 WebShell: ${t.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            }
        }
        btnRow.addView(btnRefresh)
        btnRow.addView(space(12))
        btnRow.addView(btnCopy)
        btnRow.addView(space(12))
        btnRow.addView(btnOpenWebShell)

        infoView = TextView(this).apply {
            textSize = 13f
            setTextIsSelectable(true)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val scroll = ScrollView(this).apply {
            addView(infoView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        root.addView(title, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(space(8))
        root.addView(btnRow, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(space(12))
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0, 1f
        ))

        setContentView(root)
        renderInfo()
    }

    private fun space(w: Int): TextView = TextView(this).apply { width = (w * resources.displayMetrics.density).toInt() }

    private fun renderInfo() {
        val dm: DisplayMetrics = resources.displayMetrics
        val sb = StringBuilder()

        // 基本与设备
        sb.appendLine("App Package: ${packageName}")
        sb.appendLine("Android SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        sb.appendLine("Brand/Model: ${Build.BRAND} / ${Build.MODEL}")
        sb.appendLine("Locale: ${resources.configuration.locales?.get(0)}")
        sb.appendLine()

        // 屏幕与窗口
        sb.appendLine("Screen: ${dm.widthPixels} x ${dm.heightPixels} @${dm.densityDpi}dpi  (dp: ${dm.widthPixels/dm.density} x ${dm.heightPixels/dm.density})")
        val vpW = window?.decorView?.width ?: 0
        val vpH = window?.decorView?.height ?: 0
        if (vpW > 0 && vpH > 0) sb.appendLine("Viewport: ${vpW} x ${vpH}")
        sb.appendLine()

        // WebView 提供方
        val webviewPkg = try { if (Build.VERSION.SDK_INT >= 24) WebView.getCurrentWebViewPackage()?.packageName else "(API<24)" } catch (_: Throwable) { "(error)" }
        val webviewVer = try { if (Build.VERSION.SDK_INT >= 24) WebView.getCurrentWebViewPackage()?.versionName else "" } catch (_: Throwable) { "" }
        sb.appendLine("WebView Provider: $webviewPkg $webviewVer")
        sb.appendLine()

        // 网络与能力（简要）
        val conn = try { (getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).activeNetworkInfo } catch (_: Throwable) { null }
        if (conn != null) {
            sb.appendLine("Network: ${conn.typeName} / connected=${conn.isConnected}")
            sb.appendLine()
        }

        // 说明
        sb.appendLine("说明：")
        sb.appendLine("1) 该页面为原生信息面板，不使用 WebView、不会跳转。")
        sb.appendLine("2) 点击“打开 WebShell”后，才会进入 WebView 页面。")
        sb.appendLine("3) 若进入 WebShell 后仍发生跳转，多半是网页脚本尝试播放，建议先在 WebShell 页面内保持“上锁播放”。")

        infoView.text = sb.toString()
    }
}
