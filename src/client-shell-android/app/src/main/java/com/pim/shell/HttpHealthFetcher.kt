package com.pim.shell

import java.net.HttpURLConnection
import java.net.URL

object HttpHealthFetcher {
    fun fetchStatus(url: String): Int {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }
}
