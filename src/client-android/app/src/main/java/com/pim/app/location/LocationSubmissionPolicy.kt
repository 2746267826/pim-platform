package com.pim.app.location

data class LocationSubmissionDecision(
    val canSubmitManually: Boolean,
    val shouldAutoSubmit: Boolean,
    val statusLabel: String,
    val reason: String?
)

object LocationSubmissionPolicy {
    fun decide(
        horizontalAccuracyMeters: Float?,
        autoAlreadySubmitted: Boolean,
        maxUploadAccuracyMetersExclusive: Float = 50f
    ): LocationSubmissionDecision {
        if (horizontalAccuracyMeters == null) {
            return LocationSubmissionDecision(
                canSubmitManually = false,
                shouldAutoSubmit = false,
                statusLabel = "缺少水平精度",
                reason = "缺少水平精度信息，不能提交。"
            )
        }
        if (!horizontalAccuracyMeters.isFinite()) {
            return LocationSubmissionDecision(
                canSubmitManually = false,
                shouldAutoSubmit = false,
                statusLabel = "精度无效，不接受",
                reason = "定位精度无效，不能提交。"
            )
        }

        return when {
            horizontalAccuracyMeters >= maxUploadAccuracyMetersExclusive -> LocationSubmissionDecision(
                canSubmitManually = false,
                shouldAutoSubmit = false,
                statusLabel = "误差 >= ${formatAccuracyThresholdMeters(maxUploadAccuracyMetersExclusive)}m，不接受",
                reason = "误差必须小于 ${formatAccuracyThresholdMeters(maxUploadAccuracyMetersExclusive)} 米，不能提交。"
            )
            horizontalAccuracyMeters <= 10f -> LocationSubmissionDecision(
                canSubmitManually = true,
                shouldAutoSubmit = false,
                statusLabel = "误差 <= 10m，可手动提交",
                reason = null
            )
            else -> LocationSubmissionDecision(
                canSubmitManually = true,
                shouldAutoSubmit = false,
                statusLabel = "误差 < ${formatAccuracyThresholdMeters(maxUploadAccuracyMetersExclusive)}m，可手动提交",
                reason = null
            )
        }
    }
}

internal fun formatAccuracyThresholdMeters(value: Float): String =
    if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

fun decideLocationSubmission(
    horizontalAccuracyMeters: Float?,
    autoAlreadySubmitted: Boolean,
    maxUploadAccuracyMetersExclusive: Float = 50f
): LocationSubmissionDecision =
    LocationSubmissionPolicy.decide(horizontalAccuracyMeters, autoAlreadySubmitted, maxUploadAccuracyMetersExclusive)
