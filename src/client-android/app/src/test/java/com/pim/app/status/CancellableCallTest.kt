package com.pim.app.status

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CancellableCallTest {
    @Test
    fun cancellingAwaitCancelsTheUnderlyingCall() = runTest {
        val call = ControllableCall()
        val awaiting = async { call.awaitCancellableResponse() }
        runCurrent()

        awaiting.cancelAndJoin()

        assertTrue(call.cancelled.get())
    }

    @Test
    fun responseArrivingAfterCancellationIsClosed() = runTest {
        val call = ControllableCall()
        val awaiting = async { call.awaitCancellableResponse() }
        runCurrent()
        val body = TrackingResponseBody()
        val response = Response.Builder()
            .request(call.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .build()

        awaiting.cancelAndJoin()
        call.respond(response)

        assertTrue(body.closed.get())
    }
}

private class ControllableCall : Call {
    val cancelled = AtomicBoolean(false)
    private val request = Request.Builder().url("https://pim.example/health").build()
    private val callbackReady = CountDownLatch(1)
    @Volatile private var callback: Callback? = null

    override fun request(): Request = request
    override fun execute(): Response = error("Synchronous execution is not expected")
    override fun enqueue(responseCallback: Callback) {
        callback = responseCallback
        callbackReady.countDown()
    }
    override fun cancel() {
        cancelled.set(true)
    }
    override fun isExecuted(): Boolean = callback != null
    override fun isCanceled(): Boolean = cancelled.get()
    override fun timeout(): Timeout = Timeout.NONE
    override fun clone(): Call = ControllableCall()

    fun respond(response: Response) {
        check(callbackReady.await(5, TimeUnit.SECONDS))
        callback!!.onResponse(this, response)
    }
}

private class TrackingResponseBody : ResponseBody() {
    val closed = AtomicBoolean(false)
    private val trackingSource = object : ForwardingSource(Buffer().writeUtf8("body")) {
        override fun close() {
            closed.set(true)
            super.close()
        }
    }.buffer()

    override fun contentType(): MediaType? = null
    override fun contentLength(): Long = 4L
    override fun source(): BufferedSource = trackingSource
}
