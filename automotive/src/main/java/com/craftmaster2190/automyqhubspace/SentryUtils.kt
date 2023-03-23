package com.craftmaster2190.automyqhubspace

import io.sentry.Sentry

fun <T>runOrSentry(func: () -> T) {
    try {
        func()
    } catch (e: Exception) {
        Sentry.captureException(e)
    }
}

suspend fun <T>runSuspendOrSentry(func: suspend () -> T) {
    try {
        func()
    } catch (e: Exception) {
        Sentry.captureException(e)
    }
}