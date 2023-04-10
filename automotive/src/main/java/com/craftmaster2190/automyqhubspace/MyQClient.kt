package com.craftmaster2190.automyqhubspace

import com.github.diamondminer88.myq.MyQ
import com.github.diamondminer88.myq.model.MyQDevice
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

/**
 * Uses https://github.com/rushiiMachine/myq
 */
class MyQClient {
    companion object {
        val instance by lazy { MyQClient() }
    }

    lateinit var username: String
    lateinit var password: String
    var refreshToken: String? = null

    private val loggedInMyQ = SuspendCache(Duration.ofSeconds(60), {
        val myQ = MyQ()
        var loggedIn = false
        try {
            if (refreshToken != null) {
                myQ.login(refreshToken!!)
                loggedIn = true
            }
        } catch (ignored: Throwable) {
        }
        if (!loggedIn) {
            myQ.apply { login(username, password) }
        }
        refreshToken = myQ.getRefreshToken()
        myQ
    })

    private suspend fun runOrRetryWithLogin(func: suspend () -> Unit) {
        try {
            func()
        } catch (ignored: Error) {
            loggedInMyQ.invalidate()
            func()
        }
    }

    enum class GarageDoorState {
        OPEN, OPENING, CLOSED, CLOSING
    }

    fun fetchGarageDoorState(): Future<GarageDoorState> {
        val future = CompletableFuture<GarageDoorState>()
        CoroutineScope(Dispatchers.IO).launch {
            runSuspendOrSentry {
                runOrRetryWithLogin {
                    val doorState = (myQDevice()
                        .state["door_state"] as JsonPrimitive).content
                    val garageDoorState = // GarageDoorState.CLOSED
                        GarageDoorState.valueOf(doorState.toUpperCasePreservingASCIIRules())

                    future.complete(garageDoorState)
                }
            }
        }
        return future
    }

    private suspend fun myQDevice(): MyQDevice {
        val devices = loggedInMyQ.get().fetchDevices()
//        Log.i(javaClass.simpleName, "DEVICES: $devices")
        return devices.first { it.deviceFamily == "garagedoor" }
    }

    fun setGarageDoorState(garageDoorState: GarageDoorState) {
        CoroutineScope(Dispatchers.IO).launch {
            runSuspendOrSentry {
                runOrRetryWithLogin {
                    val myQDevice = myQDevice()
                    when (garageDoorState) {
                        GarageDoorState.OPEN -> {
                            loggedInMyQ.get().setGarageDoorState(myQDevice, true)
                        }
                        GarageDoorState.OPENING -> {
                            loggedInMyQ.get().setGarageDoorState(myQDevice, true)
                        }
                        GarageDoorState.CLOSED -> {
                            loggedInMyQ.get().setGarageDoorState(myQDevice, false)
                        }
                        GarageDoorState.CLOSING -> {
                            loggedInMyQ.get().setGarageDoorState(myQDevice, false)
                        }
                    }
                }
            }
        }
    }
}

