package com.signagetv.tv.data.api

import com.signagetv.tv.data.prefs.SignagePrefs
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds Retrofit/OkHttp instances on demand. The server URL lives in prefs and
 * can change at runtime (LoginActivity exposes the field), so we rebuild lazily
 * each time the URL changes.
 */
class ApiClient(
    private val prefs: SignagePrefs,
    private val deviceIdProvider: () -> String
) {
    private var cachedBase: String? = null
    private var cachedApi: SignageApi? = null
    private var cachedOk: OkHttpClient? = null

    val okHttpClient: OkHttpClient
        get() {
            ensureBuilt()
            return cachedOk!!
        }

    fun api(): SignageApi {
        ensureBuilt()
        return cachedApi!!
    }

    private fun ensureBuilt() {
        val base = normalizeBase(prefs.serverUrl)
            ?: error("Server URL not set. Login first.")
        if (cachedApi != null && cachedBase == base) return

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val ok = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(prefs, deviceIdProvider))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Long ping for keep-alive on WS connections sharing this client.
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        cachedBase = base
        cachedOk = ok
        cachedApi = retrofit.create(SignageApi::class.java)
    }

    /** Force rebuild on next call. Call when serverUrl changes. */
    fun invalidate() {
        cachedApi = null
        cachedBase = null
        cachedOk = null
    }

    companion object {
        fun normalizeBase(raw: String?): String? {
            val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val withScheme = if (s.startsWith("http://") || s.startsWith("https://")) s else "http://$s"
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}
