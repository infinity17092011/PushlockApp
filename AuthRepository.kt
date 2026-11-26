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
import java.util.concurrent.TimeUnit

/** Email + password login through Supabase Auth's REST API. */
class AuthRepository(private val prefs: Prefs) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json".toMediaType()

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val (code, obj) = post(
            "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email).put("password", password),
        )
        if (code !in 200..299) throw AuthException(errorText(obj))
        saveSession(obj)
    }

    /** Returns true if signed in immediately, false if the email must be confirmed first. */
    suspend fun signUp(email: String, password: String): Result<Boolean> = runCatching {
        val (code, obj) = post(
            "/auth/v1/signup",
            JSONObject().put("email", email).put("password", password),
        )
        if (code !in 200..299) throw AuthException(errorText(obj))
        if (obj.has("access_token")) {
            saveSession(obj)
            true
        } else {
            false
        }
    }

    /** A usable access token, refreshed when it is about to expire. Null if signed out. */
    suspend fun validToken(): String? {
        val token = prefs.accessToken ?: return null
        if (System.currentTimeMillis() < prefs.tokenExpiresAt - 60_000) return token
        val refresh = prefs.refreshToken ?: return token
        runCatching {
            val (code, obj) = post(
                "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh),
            )
            if (code in 200..299) saveSession(obj)
        }
        // If the refresh failed (e.g. offline) we still hand back the old token;
        // the server answers 401 if it really is invalid.
        return prefs.accessToken
    }

    fun signOut() = prefs.clearAccount()

    private fun saveSession(obj: JSONObject) {
        prefs.accessToken = obj.getString("access_token")
        prefs.refreshToken = obj.optString("refresh_token").ifBlank { prefs.refreshToken }
        prefs.tokenExpiresAt = System.currentTimeMillis() + obj.optLong("expires_in", 3600L) * 1000L
        obj.optJSONObject("user")?.optString("email")?.takeIf { it.isNotBlank() }?.let { prefs.email = it }
    }

    private suspend fun post(path: String, body: JSONObject): Pair<Int, JSONObject> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(Config.SUPABASE_URL + path)
                .header("apikey", Config.SUPABASE_KEY)
                .post(body.toString().toRequestBody(jsonType))
                .build()
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                resp.code to (runCatching { JSONObject(text) }.getOrDefault(JSONObject()))
            }
        }

    private fun errorText(obj: JSONObject): String =
        obj.optString("error_description").ifBlank { obj.optString("msg") }
            .ifBlank { obj.optString("message") }
            .ifBlank { "Something went wrong. Please try again." }
}
