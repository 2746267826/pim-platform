package com.pim.app.ui.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import javax.inject.Inject
import javax.inject.Singleton

interface WebViewSiteDataCleaner {
    fun clearOrigin(origin: String)
}

@Singleton
class RealWebViewSiteDataCleaner @Inject constructor() : WebViewSiteDataCleaner {
    override fun clearOrigin(origin: String) {
        runCatching {
            WebStorage.getInstance().deleteOrigin(origin)
        }
        val cookieManager = CookieManager.getInstance()
        val header = cookieManager.getCookie(origin)
        if (!header.isNullOrBlank()) {
            header.split(";")
                .map { it.trim() }
                .map { it.substringBefore('=').trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { name ->
                    runCatching {
                        cookieManager.setCookie(origin, "$name=; Path=/")
                    }
                    runCatching {
                        cookieManager.setCookie(
                            origin,
                            "$name=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
                        )
                    }
                }
        }
        runCatching {
            cookieManager.flush()
        }
    }
}
