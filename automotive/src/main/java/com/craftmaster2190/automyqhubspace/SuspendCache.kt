package com.craftmaster2190.automyqhubspace

import io.sentry.Hint
import io.sentry.Sentry
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class SuspendCache<T>(
    val duration: Duration,
    val supplier: suspend () -> T,
    val nowFunction: () -> Long = { System.currentTimeMillis() }
) : AutoCloseable {
    var lastFetch = 0L
    var cachedValue: T? = null
    private val suspendGuard = newSingleThreadContext("suspendGuard")

    suspend fun get(): T {
        if (Duration.between(
                Instant.ofEpochMilli(lastFetch), Instant.ofEpochMilli(nowFunction())
            ) > duration
        ) {
            withContext(suspendGuard) {
                try {
                    cachedValue = supplier()
                    lastFetch = nowFunction()
                } catch (e: Throwable) {
                    invalidate()
                    throw e
                }
            }
        }
        return cachedValue!!
    }

    fun invalidate() {
        lastFetch = 0L
        cachedValue = null
    }

    override fun close() {
        suspendGuard.close()
    }
}