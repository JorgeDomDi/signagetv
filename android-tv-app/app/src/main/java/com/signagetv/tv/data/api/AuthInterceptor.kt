package com.signagetv.tv.data.api

import com.signagetv.tv.data.prefs.SignagePrefs
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects Authorization (JWT) and X-Device-Id on every request.
 * The login endpoint won't have a token yet — Authorization is only added if present.
 */
class AuthInterceptor(
    private val prefs: SignagePrefs,
    private val deviceIdProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("X-Device-Id", deviceIdProvider())
            .header("Accept", "application/json")

        prefs.token?.takeIf { it.isNotBlank() }?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
