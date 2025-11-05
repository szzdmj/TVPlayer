package com.brouken.player

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SafeStubActivity : AppCompatActivity() {

    private lateinit var infoView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 纯原生 UI，不依赖任何 WebView，不做任何外跳
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "Debug 启动占位页（SafeStub）"
            textSize = 18f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }

        val btnRefresh = Button(this).apply {
            text = "刷新信息"
            setOnClickListener { renderInfo() }
        }
        val btnOpenWebShell = Button(this).apply {
            text = "打开 WebShell"
            setOnClickListener {
                try {
                    startActivity(Intent(this@SafeStubActivity, WebShellActivity::class.java))
                } catch (t: Throwable) {
                    Toast.makeText(this@SafeStubActivity, "无法打开 WebShell: ${t.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            }
        }
        btnRow.addView(btnRefresh)
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

    private fun space(dp: Int): TextView = TextView(this).apply {
        width = (dp * resources.displayMetrics.density).toInt()
    }

    private fun renderInfo() {
        val dm: DisplayMetrics = resources.displayMetrics
        val sb = StringBuilder()
        sb.appendLine("包名: $packageName")
        sb.appendLine("SDK: ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})")
        sb.appendLine("设备: ${android.os.Build.BRAND} / ${android.os.Build.MODEL}")
        sb.appendLine("屏幕: ${dm.widthPixels} x ${dm.heightPixels} @${dm.densityDpi}dpi")
        sb.appendLine("说明：")
        sb.appendLine("• 这是 Debug 专用的启动占位页。")
        sb.appendLine("• 点击“打开 WebShell”可进一步测试 WebView 页面。")
        sb.appendLine("• 若连此页都无法停留，请使用 adb 命令直接启动本 Activity 并抓取日志。")
        sb.appendLine()
        sb.appendLine("adb 启动命令：")
        sb.appendLine("adb shell am start -W -n com.brouken.player/.SafeStubActivity")
        sb.appendLine("adb 日志建议：")
        sb.appendLine("adb logcat -d | grep -iE \"ActivityTaskManager|am_create_activity|am_on_resume_called|WebShell|SafeStub|WebView|chromium|CRASH\"")
        infoView.text = sb.toString()
    }
}
