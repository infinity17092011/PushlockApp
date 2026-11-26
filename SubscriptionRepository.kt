package com.pushlock.app.net

import com.pushlock.app.Config
import com.pushlock.app.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

data class SubStatus(val hasAccess: Boolean, val status: String, val periodEnd: String?)

/** Talks to our own server (server.js) which talks to PayPal + Supabase. */
class SubscriptionRepository(private val prefs: Prefs, private val auth: AuthRepository) {
    // Render's free plan can take ~a minute to wake up, so be patient.
    private val http = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json".toMediaType()

    suspend fun status(): Result<SubStatus> = runCatching { cache(parse(call("GET", "/api/subscriptions/status"))) }
    suspend fun sync(): Result<SubStatus> = runCatching { cache(parse(call("POST", "/api/subscriptions/sync"))) }
    suspend fun cancel(): Result<SubStatus> = runCatching { cache(parse(call("POST", "/api/subscriptions/cancel"))) }

    /** Returns the PayPal approval URL, or null if the user already has an active subscription. */
    suspend fun createCheckout(): Result<String?> = runCatching {
        val obj = call("POST", "/api/subscriptions/create")
        val url = obj.optString("approve_url")
        if (url.isBlank()) {
            cache(parse(obj))
            null
        } else {
            url
        }
    }

    private fun parse(obj: JSONObject) = SubStatus(
        hasAccess = obj.optBoolean("has_access", false),
        status = obj.optString("status", "NONE"),
        periodEnd = if (obj.isNull("current_period_end")) null else obj.optString("current_period_end").ifBlank { null },
    )

    private fun cache(s: SubStatus): SubStatus {
        prefs.subActive = s.hasAccess
        prefs.subVerifiedAt = System.currentTimeMillis()
        prefs.subPeriodEnd = s.periodEnd
            ?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0L
        return s
    }

    private suspend fun call(method: String, path: String): JSONObject = withContext(Dispatchers.IO) {
        val token = auth.validToken() ?: throw SessionExpiredException()
        val builder = Request.Builder()
            .url(Config.SERVER_URL + path)
            .header("Authorization", "Bearer $token")
        val request = if (method == "GET") builder.get().build()
        else builder.post("{}".toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            when {
                resp.code == 401 -> {
                    auth.signOut()
                    throw SessionExpiredException()
                }
                resp.code == 409 -> obj // "already subscribed": body carries the status
                !resp.isSuccessful -> throw ServerException(obj.optString("error").ifBlank { "Server error (${resp.code})" })
                else -> obj
            }
        }
    }
}
