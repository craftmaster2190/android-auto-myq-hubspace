package com.craftmaster2190.automyqhubspace

import android.util.Log
import com.google.common.base.Suppliers
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.collections.set


/**
 * Ported from https://github.com/jdeath/Hubspace-Homeassistant/blob/main/custom_components/hubspace/hubspace.py
 */
class HubSpaceClient {
    companion object {
        val instance by lazy { HubSpaceClient() }
    }

    lateinit var username: String
    lateinit var password: String

    val okHttpClient = OkHttpClient()

    val authTokenCache =
        Suppliers.memoizeWithExpiration({ getAuthTokenFromRefreshToken()!! }, 60, TimeUnit.SECONDS)

    val refreshTokenCache =
        Suppliers.memoizeWithExpiration({ getRefreshCode()!! }, 60, TimeUnit.SECONDS)

    val accountIdCache = Suppliers.memoize { getAccountId() }
    val deviceIdCache = Suppliers.memoize { getMetadeviceInfo().getString("id") }

    fun getCodeChallengeAndVerifier(): Array<String> {
        var code_verifier: String = Base64.getUrlEncoder().encodeToString(ByteArray(40))
        code_verifier = code_verifier.replace("[^a-zA-Z0-9]+".toRegex(), "")
        val digest = MessageDigest.getInstance("SHA-256")
        val hash: ByteArray = digest.digest(code_verifier.toByteArray(StandardCharsets.UTF_8))
        var code_challenge: String = Base64.getUrlEncoder().encodeToString(hash)
        code_challenge = code_challenge.replace("=", "")
        return arrayOf(code_challenge, code_verifier)
    }

    fun getAuthTokenFromRefreshToken(): String? {
        val auth_url =
            "https://accounts.hubspaceconnect.com/auth/realms/thd/protocol/openid-connect/token"
        val auth_header = Headers.headersOf(
            "Content-Type", "application/x-www-form-urlencoded",
            "user-agent", "Dart/2.15 (dart:io)",
            "host", "accounts.hubspaceconnect.com"
        )
        val auth_data = FormBody.Builder()
        auth_data.add("grant_type", "refresh_token")
        auth_data.add("refresh_token", refreshTokenCache.get())
        auth_data.add("scope", "openid email offline_access profile")
        auth_data.add("client_id", "hubspace_android")

        return okHttpClient.newCall(
            Request.Builder().post(auth_data.build())
                .headers(auth_header).url(auth_url).build()
        ).execute()
            .body.use {
                it?.string()
            }
            ?.let {
                JSONObject(it).get("id_token").toString()
            }
    }

    fun getAccountId(): String? {
        val token = authTokenCache.get()
        val auth_url = "https://api2.afero.net/v1/users/me"
        val auth_header = Headers.headersOf(
            "user-agent", "Dart/2.15 (dart:io)",
            "host", "api2.afero.net",
            "accept-encoding", "gzip",
            "authorization", "Bearer $token"
        )

        val json =
            okHttpClient.newCall(Request.Builder().get().url(auth_url).headers(auth_header).build())
                .execute()
                .body.use {
                    it?.string()
                }
                ?.let {
                    JSONObject(it)
                }!!

        val accountAccess = json.getJSONArray("accountAccess")
        val account = accountAccess.getJSONObject(0).getJSONObject("account")
        return account.getString("accountId")
    }

    fun getRefreshCode(): String? {
        val authUrl =
            "https://accounts.hubspaceconnect.com/auth/realms/thd/protocol/openid-connect/auth"
        val codeChallengeAndVerifier = getCodeChallengeAndVerifier()
        val codeChallenge = codeChallengeAndVerifier[0]
        val codeVerifier = codeChallengeAndVerifier[1]
        // defining a params dict for the parameters to be sent to the API
        val queryParams: MutableMap<String, String> = HashMap()
        queryParams["response_type"] = "code"
        queryParams["client_id"] = "hubspace_android"
        queryParams["redirect_uri"] = "hubspace-app://loginredirect"
        queryParams["code_challenge"] = codeChallenge
        queryParams["code_challenge_method"] = "S256"
        queryParams["scope"] = "openid offline_access"
        val builtUrl = authUrl.toHttpUrlOrNull()?.newBuilder()?.also {
            queryParams.forEach { key, va -> it.addQueryParameter(key, va) }
        }?.build()!!

        // sending get request and saving the response as response object
        val firstResponse =
            okHttpClient.newCall(Request.Builder().get().url(builtUrl).build()).execute()
        val body = firstResponse.body.use {
            it?.string()
        }!!

        val session_code: String = body.split("session_code=").get(1).split("&").get(0)
        val execution: String = body.split("execution=").get(1).split("&").get(0)
        val tab_id: String = body.split("tab_id=").get(1).split("&").get(0)


        val auth_url =
            "https://accounts.hubspaceconnect.com/auth/realms/thd/login-actions/authenticate?session_code=$session_code&execution=$execution&client_id=hubspace_android&tab_id=$tab_id"

        val Cookielist: List<String> = firstResponse.headers.values("Set-Cookie")
        val auth_header = Headers.headersOf(
            "Content-Type", "application/x-www-form-urlencoded",
            "user-agent",
            "Mozilla/5.0 (Linux; Android 7.1.1; Android SDK built for x86_64 Build/NYC) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36",
            "Cookie", Cookielist.joinToString(";")

        )
        val auth_data = FormBody.Builder()
        auth_data.add("username", username)
        auth_data.add("password", password)
        auth_data.add("credentialId", "")

        val r2 = okHttpClient.newCall(
            Request.Builder()
                .post((auth_data).build())
                .url(auth_url)
                .headers(auth_header)
                .build()
        ).execute()

        r2.body.use {
            it?.string()
        }

        val location: String = r2.headers.values("location")[0]


//        val session_state =
//            location.split("session_state=").toTypedArray()[1].split("&code").toTypedArray()[0]
        val code = location.split("&code=").toTypedArray()[1]


        val auth_url2 =
            "https://accounts.hubspaceconnect.com/auth/realms/thd/protocol/openid-connect/token"
        val auth_header2 = Headers.headersOf(
            "Content-Type", "application/x-www-form-urlencoded",
            "user-agent", "Dart/2.15 (dart:io)",
            "host", "accounts.hubspaceconnect.com"
        )

        val auth_data2 = FormBody.Builder()
        auth_data2.add("grant_type", "authorization_code")
        auth_data2.add("code", code)
        auth_data2.add("redirect_uri", "hubspace-app://loginredirect")
        auth_data2.add("code_verifier", codeVerifier)
        auth_data2.add("client_id", "hubspace_android")

        return okHttpClient.newCall(
            Request.Builder()
                .post(auth_data2.build()).url(auth_url2)
                .headers(auth_header2)
                .build()
        ).execute().body.use {
            it?.string()
        }
            ?.let {
                JSONObject(it).get("refresh_token").toString()
            }
    }

    fun getMetadeviceInfo(): JSONObject {
        val token = authTokenCache.get()
        val accountId = accountIdCache.get()

        val auth_url =
            "https://api2.afero.net/v1/accounts/$accountId/metadevices?expansions=state"
        val auth_header = Headers.headersOf(
            "user-agent", "Dart/2.15 (dart:io)",
            "host", "semantics2.afero.net",
            "accept-encoding", "gzip",
            "authorization", "Bearer $token"
        )

        val json =
            okHttpClient.newCall(Request.Builder().get().url(auth_url).headers(auth_header).build())
                .execute()
                .body.use {
                    it!!.string()
                }.let {
                    val jsonArray = JSONArray(it)
                    iterateJson(jsonArray)
                        .filter { device ->
                            device.optString("typeId") == "metadevice.device" &&
                                    device.optJSONObject("description")?.optJSONObject("device")
                                        ?.optString("deviceClass") == "door-lock"
                        }.findFirst()
                        .orElseThrow { IllegalStateException("No door-lock device") }
                }

        // friendlyName
        // typeId -> metadevice.device
        // description -> device -> deviceClass -> door-lock
        // state -> values[] {"functionClass":"lock-control","value":"unlocked","lastUpdateTime":1678845003703}
        //                   {"functionClass":"lock-control","value":"locked","lastUpdateTime":1678845320601}
        return json;
    }

    fun fetchDeviceState(): FrontDoorState {
        return iterateJson(
            getMetadeviceInfo()
                .getJSONObject("state")
                .getJSONArray("values")
        )
            .filter { stateValue -> stateValue.optString("functionClass") == "lock-control" }
            .findFirst()
            .map { stateValueLockControl -> stateValueLockControl.getString("value") }
            .map { lockControlState -> FrontDoorState.valueOf(lockControlState.toUpperCasePreservingASCIIRules()) }
            .orElseThrow { IllegalStateException("Unable to get lock-control state") }
    }

    fun iterateJson(jsonArray: JSONArray): Stream<JSONObject> {
        return IntStream.range(0, jsonArray.length())
            .mapToObj(jsonArray::getJSONObject)
    }

    fun setDeviceState(doorState: FrontDoorState) {
        CoroutineScope(Dispatchers.IO).launch {
            val accountId = accountIdCache.get()
            val child = deviceIdCache.get()
            val token = authTokenCache.get()

            val auth_url = "https://api2.afero.net/v1/accounts/$accountId/metadevices/$child/state"
            val contentType = "application/json; charset=utf-8"
            val auth_header = Headers.headersOf(
                "user-agent", "Dart/2.15 (dart:io)",
                "host", "semantics2.afero.net",
                "accept-encoding", "gzip",
                "authorization", "Bearer $token",
                "content-type", contentType,
            )

            val lockState = when (doorState) {
                FrontDoorState.LOCKED -> "locking"
                FrontDoorState.LOCKING -> "locking"
                FrontDoorState.UNLOCKED -> "unlocking"
                FrontDoorState.UNLOCKING -> "unlocking"
            }

            val requestBodyJson = JSONObject()
                .put("metadeviceId", child)
                .put(
                    "values", JSONArray().put(
                        JSONObject()
                            .put("functionClass", "lock-control")
                            .put("lastUpdateTime", Instant.now().epochSecond)
                            .put("value", lockState)
                    )
                )
                .toString()
            Log.i(javaClass.simpleName, "Sending $requestBodyJson")
            val requestBody = requestBodyJson
                .toRequestBody(contentType.toMediaType())

            okHttpClient.newCall(
                Request.Builder().put(requestBody)
                    .url(auth_url).headers(auth_header).build()
            ).execute().let { response ->
                response.body.use { responseBody ->
                    Log.i(
                        javaClass.simpleName,
                        "setDeviceState response ${response.code} ${response.headers} ${responseBody?.string()}"
                    )
                }
            }
        }
    }

    enum class FrontDoorState {
        LOCKED, UNLOCKED, LOCKING, UNLOCKING
    }
}