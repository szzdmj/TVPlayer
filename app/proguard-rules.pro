# 保留 WebShellActivity 的 JS 接口，避免被混淆/移除（Debug 如启用混淆必须保留）
-keepclassmembers class com.brouken.player.WebShellActivity$JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}
