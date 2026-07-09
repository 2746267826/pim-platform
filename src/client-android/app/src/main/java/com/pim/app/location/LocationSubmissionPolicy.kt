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
        autoAlreadySubmitted: Boolean
    ): LocationSubmissionDecision {
        if (horizontalAccuracyMeters == null) {
            return LocationSubmissionDecision(
                canSubmitManually = false,
                shouldAutoSubmit = false,
                statusLabel = "缺少水平精度",
                reason = "缺少水平精度信息，不能提交。"
            )
        }

        return when {
            horizontalAccuracyMeters <= 10f -> LocationSubmissionDecision(
                canSubmitManually = true,
                shouldAutoSubmit = !autoAlreadySubmitted,
                statusLabel = "误差 <= 10m，可自动提交",
                reason = null
            )
            horizontalAccuracyMeters < 50f -> LocationSubmissionDecision(
                canSubmitManually = true,
                shouldAutoSubmit = false,
                statusLabel = "误差 < 50m，可手动提交",
                reason = null
            )
            else -> LocationSubmissionDecision(
                canSubmitManually = false,
                shouldAutoSubmit = false,
                statusLabel = "误差 >= 50m，不接受",
                reason = "误差必须小于 50 米，不能提交。"
            )
        }
    }
}

fun decideLocationSubmission(
    horizontalAccuracyMeters: Float?,
    autoAlreadySubmitted: Boolean
): LocationSubmissionDecision =
    LocationSubmissionPolicy.decide(horizontalAccuracyMeters, autoAlreadySubmitted)
