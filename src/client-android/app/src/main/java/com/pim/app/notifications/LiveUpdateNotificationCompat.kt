package com.pim.app.notifications

import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Best-effort Live Updates promote path for API 36+.
 * Never throws; falls back to the unmodified builder on any failure.
 */
object LiveUpdateNotificationCompat {
    private const val MIN_SDK = 36
    private const val SHORT_CRITICAL_MAX = 7
    private const val PROGRESS_MAX = 100

    fun applyIfSupported(
        builder: NotificationCompat.Builder,
        model: LocationNotificationUiModel
    ): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT < MIN_SDK) return builder
        if (!model.requestLiveUpdate || !model.isOngoing) return builder
        return try {
            applyApi36(builder, model)
        } catch (_: Throwable) {
            builder
        }
    }

    private fun applyApi36(
        builder: NotificationCompat.Builder,
        model: LocationNotificationUiModel
    ): NotificationCompat.Builder {
        builder.setColorized(true)
        invokeCompatBuilderMethod(builder, "setRequestPromotedOngoing", arrayOf(Boolean::class.javaPrimitiveType!!), arrayOf(true))
        invokeCompatBuilderMethod(
            builder,
            "setShortCriticalText",
            arrayOf(CharSequence::class.java),
            arrayOf(model.shortStatus.take(SHORT_CRITICAL_MAX))
        )
        if (!applyProgressStyle(builder, model.progressPercent)) {
            val percent = model.progressPercent
            if (percent != null) {
                builder.setProgress(PROGRESS_MAX, percent.coerceIn(0, PROGRESS_MAX), false)
            }
        }
        return builder
    }

    private fun applyProgressStyle(
        builder: NotificationCompat.Builder,
        progressPercent: Int?
    ): Boolean {
        return try {
            val styleClass = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle")
            val style = styleClass.getDeclaredConstructor().newInstance()
            val point = progressPercent?.coerceIn(0, PROGRESS_MAX) ?: 0
            invokeOptional(style, "setProgress", arrayOf(Int::class.javaPrimitiveType!!), arrayOf(point))
            invokeOptional(style, "setProgressMax", arrayOf(Int::class.javaPrimitiveType!!), arrayOf(PROGRESS_MAX))
            invokeOptional(style, "setProgressIndeterminate", arrayOf(Boolean::class.javaPrimitiveType!!), arrayOf(progressPercent == null))
            builder.setStyle(style as NotificationCompat.Style)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun invokeCompatBuilderMethod(
        builder: NotificationCompat.Builder,
        name: String,
        paramTypes: Array<Class<*>>,
        args: Array<Any?>
    ) {
        try {
            val method = NotificationCompat.Builder::class.java.getMethod(name, *paramTypes)
            method.invoke(builder, *args)
        } catch (_: Throwable) {
            // Method absent on current AndroidX; keep notification usable.
        }
    }

    private fun invokeOptional(
        target: Any,
        name: String,
        paramTypes: Array<Class<*>>,
        args: Array<Any?>
    ) {
        try {
            val method = target.javaClass.getMethod(name, *paramTypes)
            method.invoke(target, *args)
        } catch (_: Throwable) {
            // Optional ProgressStyle setter missing; ignore.
        }
    }
}
