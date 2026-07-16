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
        // Keep existing BigTextStyle from Renderer; do not replace with ProgressStyle.
        builder.setColorized(true)
        invokeCompatBuilderMethod(
            builder,
            "setRequestPromotedOngoing",
            arrayOf(Boolean::class.javaPrimitiveType!!),
            arrayOf(true)
        )
        invokeCompatBuilderMethod(
            builder,
            "setShortCriticalText",
            arrayOf(CharSequence::class.java),
            arrayOf(model.shortStatus.take(SHORT_CRITICAL_MAX))
        )
        val percent = model.progressPercent
        if (percent != null) {
            builder.setProgress(PROGRESS_MAX, percent.coerceIn(0, PROGRESS_MAX), false)
        }
        return builder
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

}
