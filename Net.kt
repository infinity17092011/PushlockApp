package com.pushlock.app.net

import java.io.IOException

class AuthException(message: String) : Exception(message)
class SessionExpiredException : Exception("Please sign in again.")
class ServerException(message: String) : Exception(message)

/** Turns any failure into something friendly to show on screen. */
fun Throwable.userMessage(): String = when (this) {
    is AuthException, is ServerException, is SessionExpiredException -> message ?: "Something went wrong."
    is IOException -> "Can't reach the server. Check your connection and try again."
    else -> message ?: "Something went wrong."
}
