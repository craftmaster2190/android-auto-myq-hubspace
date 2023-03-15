package com.craftmaster2190.automyqhubspace

import android.util.Log
import com.github.diamondminer88.myq.MyQ
import com.github.diamondminer88.myq.model.MyQDevice
import com.google.common.base.Suppliers
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Uses https://github.com/rushiiMachine/myq
 */
class MyQClient {
    companion object {
        val instance by lazy { MyQClient() }
    }

    lateinit var username: String
    lateinit var password: String

    private val myQ = MyQ()

    private val loggedInMyQ_ = Suppliers.memoizeWithExpiration({
        suspend {
            loginMyQ()
            myQ
        }
    }, 60, TimeUnit.SECONDS)

    private suspend fun loggedInMyQ(): MyQ {
        return loggedInMyQ_.get()()
    }


    private suspend fun loginMyQ() {
        try {
            myQ.getRefreshToken().let { myQ.login(it) }
        } catch (ignored: Error) {
            myQ.login(username, password)
        } catch (ignored: Exception) {
            myQ.login(username, password)
        }
    }

    private suspend fun runWithLogin(func: suspend () -> Unit) {
        try {
            func()
        } catch (ignored: Error) {
            loginMyQ()
            func()
        }
    }

    enum class GarageDoorState {
        OPEN, OPENING, CLOSED, CLOSING
    }

    fun fetchGarageDoorState(): Future<GarageDoorState> {
        val future = CompletableFuture<GarageDoorState>()
        CoroutineScope(Dispatchers.IO).launch {
            runWithLogin {
                val doorState = (myQDevice()
                    .state["door_state"] as JsonPrimitive).content
                val garageDoorState = // GarageDoorState.CLOSED
                    GarageDoorState.valueOf(doorState.toUpperCasePreservingASCIIRules())

                future.complete(garageDoorState)
            }
        }
        return future
    }

    private suspend fun myQDevice(): MyQDevice {
        val devices = loggedInMyQ().fetchDevices()
//        Log.i(javaClass.simpleName, "DEVICES: $devices")
        return devices.first { it.deviceFamily == "garagedoor" }
    }

    fun setGarageDoorState(garageDoorState: GarageDoorState) {
        CoroutineScope(Dispatchers.IO).launch {
            runWithLogin {
                val myQDevice = myQDevice()
                when (garageDoorState) {
                    GarageDoorState.OPEN -> {
                        loggedInMyQ().setGarageDoorState(myQDevice, true)
                    }
                    GarageDoorState.CLOSED -> {
                        loggedInMyQ().setGarageDoorState(myQDevice, false)
                    }
                    else ->
                        Log.e(
                            javaClass.simpleName,
                            "Unable to change garage door to $garageDoorState"
                        )
                }
            }
        }
    }
}