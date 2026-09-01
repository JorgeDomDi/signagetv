package com.signagetv.tv.data.repository

import android.content.Context
import com.signagetv.tv.data.api.ApiClient
import com.signagetv.tv.data.api.AssignPlaylistRequest
import com.signagetv.tv.data.api.CurrentPlaylistResponse
import com.signagetv.tv.data.api.LoginRequest
import com.signagetv.tv.data.api.LoginResponse
import com.signagetv.tv.data.api.PlaylistDto
import com.signagetv.tv.data.api.RegisterTvRequest
import com.signagetv.tv.data.api.TvDto
import com.signagetv.tv.data.prefs.SignagePrefs
import com.signagetv.tv.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Single entry point for the UI layer. Wraps the Retrofit API, persists state in
 * prefs, and handles local media caching.
 */
class SignageRepository(
    private val appContext: Context,
    private val apiClient: ApiClient,
    val prefs: SignagePrefs
) {

    suspend fun login(serverUrl: String, username: String, password: String): LoginResponse =
        withContext(Dispatchers.IO) {
            prefs.serverUrl = serverUrl
            apiClient.invalidate()
            val res = apiClient.api().login(LoginRequest(username, password))
            prefs.token = res.token
            prefs.localId = res.local.id
            prefs.localName = res.local.nombre
            res
        }

    suspend fun registerTv(deviceId: String, nombre: String): TvDto =
        withContext(Dispatchers.IO) {
            val tv = apiClient.api().registerTv(RegisterTvRequest(deviceId, nombre))
            prefs.tvId = tv.id
            tv
        }

    suspend fun listPlaylists(): List<PlaylistDto> =
        withContext(Dispatchers.IO) { apiClient.api().listPlaylists() }

    suspend fun assignPlaylist(tvId: Long, playlistId: Long?): TvDto =
        withContext(Dispatchers.IO) {
            apiClient.api().assignPlaylist(tvId, AssignPlaylistRequest(playlistId))
        }

    suspend fun getCurrentPlaylist(): CurrentPlaylistResponse =
        withContext(Dispatchers.IO) {
            val res = apiClient.api().getCurrentPlaylist()
            if (!res.isSuccessful) {
                throw java.io.IOException("El servidor respondio ${res.code()}")
            }
            // 204 o cuerpo ausente = "todavia no tenes nada asignado". No es un error.
            CurrentPlaylistResponse(playlist = res.body())
        }

    // ===== Media caching =====

    /**
     * Returns a local File for the given remote media URL, downloading if missing.
     * Cache key = sha1(url). Files live in [cacheDir]/media/.
     */
    suspend fun ensureMediaCached(url: String): File = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "media").apply { mkdirs() }
        val name = sha1(url)
        val target = File(dir, name)
        if (target.exists() && target.length() > 0) return@withContext target

        val req = okhttp3.Request.Builder().url(url).build()
        apiClient.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw java.io.IOException("Download failed ${resp.code} for $url")
            }
            val body = resp.body ?: throw java.io.IOException("Empty body for $url")
            val tmp = File(dir, "$name.tmp")
            FileOutputStream(tmp).use { out ->
                body.byteStream().copyTo(out)
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }
        Logger.d("Cached media -> ${target.absolutePath} (${target.length()} bytes)")
        target
    }

    fun logout() {
        prefs.clearAuth()
    }

    private fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
