package com.pim.app.mobile.sync

import com.pim.core.models.MobileIngestResponse

data class MobileAcknowledgementItem(
    val entityType: String,
    val clientItemKey: String
)

data class MobileAcknowledgementPlan(
    val confirmedKeys: Set<String>,
    val retryKeys: Set<String>,
    val deadLetterKeys: Set<String>,
    val failureCode: String? = null
)

data class MobileTypedAcknowledgementPlan(
    val confirmedItems: Set<MobileAcknowledgementItem> = emptySet(),
    val retryItems: Set<MobileAcknowledgementItem> = emptySet(),
    val deadLetterItems: Set<MobileAcknowledgementItem> = emptySet(),
    val failureCode: String? = null
)

object MobileAcknowledgementPlanner {
    private const val LegacyEntityType = "usage-event"
    private val KnownEntityTypes = setOf("app-metadata", "usage-event", "usage-summary")

    // Bare keys predate typed acknowledgements and represent usage-event rows.
    fun plan(
        sentKeys: Set<String>,
        response: MobileIngestResponse
    ): MobileAcknowledgementPlan {
        val typedPlan = planTyped(
            sentItems = sentKeys.mapTo(linkedSetOf()) {
                MobileAcknowledgementItem(LegacyEntityType, it)
            },
            response = response
        )
        return MobileAcknowledgementPlan(
            confirmedKeys = typedPlan.confirmedItems.mapTo(linkedSetOf()) { it.clientItemKey },
            retryKeys = typedPlan.retryItems.mapTo(linkedSetOf()) { it.clientItemKey },
            deadLetterKeys = typedPlan.deadLetterItems.mapTo(linkedSetOf()) { it.clientItemKey },
            failureCode = typedPlan.failureCode
        )
    }

    fun planTyped(
        sentItems: Set<MobileAcknowledgementItem>,
        response: MobileIngestResponse
    ): MobileTypedAcknowledgementPlan {
        if (sentItems.any { it.entityType !in KnownEntityTypes || it.clientItemKey.isBlank() }) {
            return ambiguous(sentItems)
        }

        if (response.itemResults.isEmpty()) {
            val aggregateIsComplete = response.acceptedCount >= 0 &&
                response.skippedCount >= 0 &&
                response.acceptedCount + response.skippedCount == sentItems.size &&
                response.rejectedCount == 0 &&
                response.failedCount == 0

            return if (aggregateIsComplete) {
                typedPlan(confirmedItems = sentItems)
            } else {
                ambiguous(sentItems)
            }
        }

        if (!explicitCountsMatch(response)) {
            return ambiguous(sentItems)
        }

        val typedResults = response.itemResults.map { result ->
            MobileAcknowledgementItem(result.entityType, result.clientItemKey) to result
        }
        val hasUnexpectedResult = typedResults.any { (item, _) ->
            item.entityType !in KnownEntityTypes ||
                item.clientItemKey.isBlank() ||
                item !in sentItems
        }
        if (hasUnexpectedResult) {
            return ambiguous(sentItems)
        }

        val resultsByItem = typedResults.groupBy(
            keySelector = { (item, _) -> item },
            valueTransform = { (_, result) -> result }
        )
        val confirmed = linkedSetOf<MobileAcknowledgementItem>()
        val retry = linkedSetOf<MobileAcknowledgementItem>()
        val deadLetter = linkedSetOf<MobileAcknowledgementItem>()
        var hasAmbiguity = false

        for (item in sentItems) {
            val itemResults = resultsByItem[item]
            if (itemResults?.size != 1) {
                retry += item
                hasAmbiguity = true
                continue
            }

            when (itemResults.single().outcome) {
                "accepted", "skipped" -> confirmed += item
                "rejected" -> deadLetter += item
                "failed" -> retry += item
                else -> {
                    retry += item
                    hasAmbiguity = true
                }
            }
        }

        return typedPlan(
            confirmedItems = confirmed,
            retryItems = retry,
            deadLetterItems = deadLetter,
            failureCode = if (hasAmbiguity) "server-ack-ambiguous" else null
        )
    }

    private fun ambiguous(sentItems: Set<MobileAcknowledgementItem>) = typedPlan(
        retryItems = sentItems,
        failureCode = "server-ack-ambiguous"
    )

    private fun explicitCountsMatch(response: MobileIngestResponse): Boolean {
        val accepted = response.itemResults.count { it.outcome == "accepted" }
        val skipped = response.itemResults.count { it.outcome == "skipped" }
        val rejected = response.itemResults.count { it.outcome == "rejected" }
        val failed = response.itemResults.count { it.outcome == "failed" }
        return response.acceptedCount == accepted &&
            response.skippedCount == skipped &&
            response.rejectedCount == rejected &&
            response.failedCount == failed &&
            accepted + skipped + rejected + failed == response.itemResults.size
    }

    private fun typedPlan(
        confirmedItems: Set<MobileAcknowledgementItem> = emptySet(),
        retryItems: Set<MobileAcknowledgementItem> = emptySet(),
        deadLetterItems: Set<MobileAcknowledgementItem> = emptySet(),
        failureCode: String? = null
    ) = MobileTypedAcknowledgementPlan(
        confirmedItems = confirmedItems,
        retryItems = retryItems,
        deadLetterItems = deadLetterItems,
        failureCode = failureCode
    )
}
