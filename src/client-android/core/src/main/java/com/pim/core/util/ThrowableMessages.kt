package com.pim.core.util

fun Throwable.toCauseChainMessage(maxDepth: Int = 6): String {
    return generateSequence(this) { it.cause }
        .take(maxDepth.coerceAtLeast(1))
        .map { throwable ->
            val typeName = throwable::class.java.simpleName.ifBlank { throwable::class.java.name }
            val message = throwable.message
                ?.takeIf { it.isNotBlank() }
                ?.let { if (it.length > MAX_MESSAGE_LENGTH) it.take(MAX_MESSAGE_LENGTH) + "..." else it }

            if (message == null) typeName else "$typeName: $message"
        }
        .joinToString(" -> ")
}

private const val MAX_MESSAGE_LENGTH = 500
