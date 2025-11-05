package com.brouken.player

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EmptyStubActivity : AppCompatActivity() {

    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 尽量保证页面留在前台，避免亮灭屏/锁屏干扰
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // 纯原生 UI，不做任何外跳
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.START
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "WebShell 占位页（EmptyStubActivity）"
            textSize = 18f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnOpen = Button(this).apply {
            text = "打开 WebShell"
            setOnClickListener {
                try {
                    startActivity(Intent(this@EmptyStubActivity, WebShellActivity::class.java))
                } catch (_: Throwable) {
                    // 保持当前页，不做任何跳转
                }
            }
        }

        val btnClose = Button(this).apply {
            text = "关闭"
            setOnClickListener { finish() }
        }

        btnRow.addView(btnOpen)
        btnRow.addView(space(12))
        btnRow.addView(btnClose)

        info = TextView(this).apply {
            textSize = 13f
            setTextIsSelectable(true)
            text = buildString {
                appendLine("说明：")
                appendLine("• 该页面不使用 WebView，不会跳转。")
                appendLine("• 点击“打开 WebShell”再进入 WebView 壳调试。")
                appendLine("• 若点“打开 WebShell”后仍立刻被切走，返回本页继续观察并抓取日志。")
            }
        }

        val scroll = ScrollView(this).apply {
            addView(info, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        root.addView(title)
        root.addView(space(8))
        root.addView(btnRow)
        root.addView(space(12))
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0, 1f
        ))

        setContentView(root)
    }

    private fun space(dp: Int): TextView = TextView(this).apply {
        width = (dp * resources.displayMetrics.density).toInt()
    }
}
