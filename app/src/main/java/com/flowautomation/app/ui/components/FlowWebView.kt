package com.flowautomation.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.flowautomation.app.bridge.WebAppInterface
import java.io.InputStream

class WebViewController(private val webView: WebView?) {
    fun eval(script: String) {
        webView?.post { webView.evaluateJavascript(script, null) }
    }

    fun startAutomation() {
        eval("window.__flowAutomation?.start()")
    }

    fun stopAutomation() {
        eval("window.__flowAutomation?.stop()")
    }

    fun clearCache() {
        eval("window.__flowAutomation?.clearCache()")
    }

    fun setZoom(factor: Float) {
        eval("window.__flowAutomation?.setZoom($factor)")
    }

    fun reload() {
        webView?.reload()
    }

    fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FlowWebView(
    bridge: WebAppInterface,
    onControllerReady: (WebViewController) -> Unit = {},
    onPageStarted: () -> Unit = {},
    onPageFinished: (String) -> Unit = {},
    onProgress: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnReady = rememberUpdatedState(onControllerReady)

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                userAgentString = settings.userAgentString.replace(
                    "Android", "Chrome/120.0.6099.230"
                )
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            addJavascriptInterface(bridge, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onPageStarted()
                    url?.let { onProgress(0) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        onPageFinished(it)
                        injectAutomationScripts(view)
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    return false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onProgress(newProgress)
                }
            }

            loadUrl("https://labs.google")
        }
    }

    DisposableEffect(Unit) {
        currentOnReady.value(WebViewController(webView))
        onDispose { webView.destroy() }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}

private fun injectAutomationScripts(webView: WebView?) {
    val context = webView?.context ?: return
    val scripts = listOf("js/automation.js", "js/file-upload-interceptor.js")
    for (assetPath in scripts) {
        try {
            val input: InputStream = context.assets.open(assetPath)
            val script = input.bufferedReader().use { it.readText() }
            webView.evaluateJavascript(script, null)
        } catch (_: Exception) { }
    }
}
