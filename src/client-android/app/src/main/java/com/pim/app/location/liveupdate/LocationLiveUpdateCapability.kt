package com.pim.app.location.liveupdate

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi

object LocationLiveUpdateCapability {

    @SuppressLint("NewApi")
    fun isAvailable(): Boolean = supportsLiveUpdates(Build.VERSION.SDK_INT) {
        Api36.atLeastBaklava1()
    }

    internal fun supportsLiveUpdates(majorSdk: Int, fullSdkCheck: () -> Boolean): Boolean {
        if (majorSdk < 36) return false
        if (majorSdk > 36) return true
        return try {
            fullSdkCheck()
        } catch (e: LinkageError) {
            false
        }
    }

    fun check(): Boolean = isAvailable()

    @RequiresApi(36)
    @SuppressLint("NewApi")
    private object Api36 {
        fun atLeastBaklava1(): Boolean =
            Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
    }
}
