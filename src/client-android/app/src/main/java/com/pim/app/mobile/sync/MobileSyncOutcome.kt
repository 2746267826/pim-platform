package com.pim.app.mobile.sync

import androidx.work.ListenableWorker
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class MobileSyncOutcome {
    SUCCESS,
    RETRY,
    BLOCKED
}

internal object MobileSyncErrorClassifier {
    fun classify(throwable: Throwable): MobileSyncOutcome {
        if (throwable is CancellationException) throw throwable
        return when (throwable) {
            is HttpException -> {
                when (throwable.code()) {
                    408, 429, in 500..599 -> MobileSyncOutcome.RETRY
                    in 400..499 -> MobileSyncOutcome.BLOCKED
                    else -> MobileSyncOutcome.RETRY
                }
            }
            is SocketTimeoutException, is ConnectException,
            is UnknownHostException, is SocketException, is IOException ->
                MobileSyncOutcome.RETRY
            else -> MobileSyncOutcome.BLOCKED
        }
    }
}

internal fun mapOutcomeToWorkerResult(outcome: MobileSyncOutcome): ListenableWorker.Result {
    return when (outcome) {
        MobileSyncOutcome.SUCCESS -> ListenableWorker.Result.success()
        MobileSyncOutcome.RETRY -> ListenableWorker.Result.retry()
        MobileSyncOutcome.BLOCKED -> ListenableWorker.Result.failure()
    }
}
