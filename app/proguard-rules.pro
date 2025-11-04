# 保留 WebShellActivity 的 JS 接口方法，避免被混淆/移除
-keepclassmembers class com.brouken.player.WebShellActivity$JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}
