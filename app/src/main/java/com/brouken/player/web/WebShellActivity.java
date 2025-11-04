package com.brouken.player.web;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * WebShellActivity
 * - 面向 Android 5.0+（API 21）TV
 * - 统一 window.open / target=_blank 在当前 WebView 打开
 * - 默认加载 file:///android_asset/index.html，或通过 Intent 传入 url
 * - file:// 资源时注入 id-shim.js；页面加载完成后若存在 window.loadWeb() 会自动尝试调用
 *
 * Intent extras:
 * - "url" (String，可选)：要加载的网页地址；未设置时默认加载本地 index.html
 */
public class WebShellActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";
    private static final String DEFAULT_ASSET_URL = "file:///android_asset/index.html";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        webView = new WebView(getApplicationContext());
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(webView);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        } catch (Throwable ignored) {}

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadsImagesAutomatically(true);
        s.setAllowFileAccess(true);
        try { s.setAllowFileAccessFromFileURLs(true); } catch (Throwable ignored) {}
        try { s.setAllowUniversalAccessFromFileURLs(true); } catch (Throwable ignored) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                // 统一在当前 WebView 打开（主要依赖 onPageFinished 注入脚本）
                return false;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(view, url);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= 21 && request != null && request.getUrl() != null) {
                    return handleUrl(view, String.valueOf(request.getUrl()));
                }
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectCompatJs(view);
            }
        });

        String url = getIntentString(getIntent(), EXTRA_URL, null);
        if (url == null || url.trim().isEmpty()) {
            url = DEFAULT_ASSET_URL;
        }
        webView.loadUrl(url);
    }

    private boolean handleUrl(WebView v, String url) {
        if (url == null || url.length() == 0) return true;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            v.loadUrl(url);
            return true;
        }
        if (url.startsWith("#") || url.startsWith("about:")) {
            return true;
        }
        v.loadUrl(url);
        return true;
    }

    private void injectCompatJs(WebView view) {
        String js =
                "(function(){try{"
                        + "window.open=function(u){if(u){location.href=u;}};"
                        + "var as=document.querySelectorAll('a[target=\"_blank\"]');"
                        + "for(var i=0;i<as.length;i++){try{as[i].setAttribute('target','_self');}catch(e){}}"
                        + "if(location && location.protocol==='file:'){"
                        + "  try{var s=document.createElement('script');s.src='id-shim.js';document.head.appendChild(s);}catch(e){}"
                        + "}"
                        + "setTimeout(function(){try{if(typeof window.loadWeb==='function')window.loadWeb();}catch(e){}},0);"
                        + "}catch(e){}})();";
        if (Build.VERSION.SDK_INT >= 19) {
            view.evaluateJavascript(js, null);
        } else {
            view.loadUrl("javascript:" + js);
        }
    }

    private static String getIntentString(Intent intent, String key, String def) {
        try {
            String v = intent != null ? intent.getStringExtra(key) : null;
            return v != null ? v : def;
        } catch (Throwable t) {
            return def;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            try { ((ViewGroup) webView.getParent()).removeView(webView); } catch (Throwable ignored) {}
            try { webView.stopLoading(); } catch (Throwable ignored) {}
            try { webView.clearHistory(); } catch (Throwable ignored) {}
            try { webView.clearCache(true); } catch (Throwable ignored) {}
            try { webView.loadUrl("about:blank"); } catch (Throwable ignored) {}
            try { webView.removeAllViews(); } catch (Throwable ignored) {}
            try { webView.destroy(); } catch (Throwable ignored) {}
            webView = null;
        }
    }

    // DPAD/ENTER -> 触发当前聚焦元素点击，适配 TV
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView != null && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            try {
                String js="(function(){try{if(document.activeElement){document.activeElement.click();}}catch(e){}})();";
                if (Build.VERSION.SDK_INT >= 19) webView.evaluateJavascript(js, null);
                else webView.loadUrl("javascript:" + js);
            } catch (Throwable ignored) {}
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
