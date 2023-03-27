package com.craftmaster2190.automyqhubspace

import android.widget.Toast
import io.sentry.Sentry

fun <T> runOrSentry(func: () -> T) {
    try {
        func()
    } catch (e: Exception) {
        Sentry.captureException(e)
        toastExceptionIfOnPhone(e)
    }
}

suspend fun <T> runSuspendOrSentry(func: suspend () -> T) {
    try {
        func()
    } catch (e: Exception) {
        Sentry.captureException(e)
        toastExceptionIfOnPhone(e)
    }
}

private fun toastExceptionIfOnPhone(e: Exception) {
    MainActivity.instance?.let { mainActivity ->
        mainActivity.runOnUiThread {
            Toast.makeText(mainActivity, "${e.message} ${e.stackTrace.getOrNull(1)}", Toast.LENGTH_LONG)
                .show()
        }
    }
}