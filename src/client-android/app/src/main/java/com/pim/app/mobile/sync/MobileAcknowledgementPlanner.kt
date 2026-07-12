package com.pim.app.mobile.sync

import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileSyncStatus
import com.pim.core.models.MobileIngestItemResult
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
    val failureCode: String? = null,
    val itemErrors: Map<MobileAcknowledgementItem, String> = emptyMap()
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
        val itemErrors = linkedMapOf<MobileAcknowledgementItem, String>()
        var hasAmbiguity = false

        for (item in sentItems) {
            val itemResults = resultsByItem[item]
            if (itemResults?.size != 1) {
                retry += item
                itemErrors[item] = "server-ack-ambiguous"
                hasAmbiguity = true
                continue
            }

            val result = itemResults.single()
            when (result.outcome) {
                "accepted", "skipped" -> confirmed += item
                "rejected" -> {
                    deadLetter += item
                    itemErrors[item] = formatItemError(result, "server-rejected")
                }
                "failed" -> {
                    retry += item
                    itemErrors[item] = formatItemError(result, "server-retry")
                }
                else -> {
                    retry += item
                    itemErrors[item] = "server-ack-ambiguous"
                    hasAmbiguity = true
                }
            }
        }

        return typedPlan(
            confirmedItems = confirmed,
            retryItems = retry,
            deadLetterItems = deadLetter,
            failureCode = if (hasAmbiguity) "server-ack-ambiguous" else null,
            itemErrors = itemErrors
        )
    }

    private fun ambiguous(sentItems: Set<MobileAcknowledgementItem>) = typedPlan(
        retryItems = sentItems,
        failureCode = "server-ack-ambiguous",
        itemErrors = sentItems.associateWith { "server-ack-ambiguous" }
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
        failureCode: String? = null,
        itemErrors: Map<MobileAcknowledgementItem, String> = emptyMap()
    ) = MobileTypedAcknowledgementPlan(
        confirmedItems = confirmedItems,
        retryItems = retryItems,
        deadLetterItems = deadLetterItems,
        failureCode = failureCode,
        itemErrors = itemErrors
    )

    private fun formatItemError(result: MobileIngestItemResult, fallback: String): String {
        val hasCode = result.code.isNotBlank()
        val hasMessage = result.message.isNotBlank()
        return when {
            hasCode && hasMessage -> "${result.code}: ${result.message}"
            hasCode -> result.code
            hasMessage -> result.message
            else -> fallback
        }
    }
}

suspend fun applyAcknowledgementPlan(
    dao: MobileDataDao,
    plan: MobileTypedAcknowledgementPlan
) {
    if (plan.confirmedItems.isNotEmpty()) {
        val eventIds = plan.confirmedItems
            .filter { it.entityType == "usage-event" }
            .mapNotNull { it.clientItemKey.toLongOrNull() }
        if (eventIds.isNotEmpty()) dao.deleteUsageEventByIds(eventIds)

        val summaryIds = plan.confirmedItems
            .filter { it.entityType == "usage-summary" }
            .mapNotNull { it.clientItemKey.toLongOrNull() }
        if (summaryIds.isNotEmpty()) dao.deleteUsageSummaryByIds(summaryIds)

        val pkgNames = plan.confirmedItems
            .filter { it.entityType == "app-metadata" }
            .map { it.clientItemKey.substringBeforeLast("@") }
        if (pkgNames.isNotEmpty()) dao.deleteAppMetadataByPackageNames(pkgNames)
    }

    plan.deadLetterItems.groupBy { it.entityType }.forEach { (entityType, items) ->
        items.groupBy { plan.itemErrors[it] ?: "server-rejected" }.forEach { (error, errorItems) ->
            when (entityType) {
                "usage-event" -> {
                    val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
                    if (ids.isNotEmpty()) dao.updateUsageEventSyncStatus(ids, MobileSyncStatus.REJECTED, error)
                }
                "usage-summary" -> {
                    val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
                    if (ids.isNotEmpty()) dao.updateUsageSummarySyncStatus(ids, MobileSyncStatus.REJECTED, error)
                }
                "app-metadata" -> {
                    val names = errorItems.map { it.clientItemKey.substringBeforeLast("@") }
                    if (names.isNotEmpty()) dao.updateAppMetadataSyncStatus(names, MobileSyncStatus.REJECTED, error)
                }
            }
        }
    }

    plan.retryItems.groupBy { it.entityType }.forEach { (entityType, items) ->
        items.groupBy { plan.itemErrors[it] ?: "server-retry" }.forEach { (error, errorItems) ->
            when (entityType) {
                "usage-event" -> {
                    val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
                    if (ids.isNotEmpty()) dao.updateUsageEventSyncStatus(ids, MobileSyncStatus.PENDING, error)
                }
                "usage-summary" -> {
                    val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
                    if (ids.isNotEmpty()) dao.updateUsageSummarySyncStatus(ids, MobileSyncStatus.PENDING, error)
                }
                "app-metadata" -> {
                    val names = errorItems.map { it.clientItemKey.substringBeforeLast("@") }
                    if (names.isNotEmpty()) dao.updateAppMetadataSyncStatus(names, MobileSyncStatus.PENDING, error)
                }
            }
        }
    }
}

suspend fun processUsageAcknowledgements(
    dao: MobileDataDao,
    sentItems: Set<MobileAcknowledgementItem>,
    response: MobileIngestResponse
) {
    val plan = MobileAcknowledgementPlanner.planTyped(sentItems, response)
    applyAcknowledgementPlan(dao, plan)
}
