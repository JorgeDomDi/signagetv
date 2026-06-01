package com.signagetv.tv.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.signagetv.tv.util.Logger

/**
 * Persists auth token, server URL, current local and the selected playlist choice.
 * Uses EncryptedSharedPreferences so the JWT is not stored in plain text.
 * Falls back to plain SharedPreferences if encryption is unavailable (e.g. broken keystore).
 */
class SignagePrefs(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "signagetv_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        Logger.w("EncryptedSharedPreferences failed, falling back to plain", t)
        context.getSharedPreferences("signagetv_secure_plain", Context.MODE_PRIVATE)
    }

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER, null)
        set(value) { prefs.edit().putString(KEY_SERVER, value).apply() }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_TOKEN, value).apply() }

    var localId: Long
        get() = prefs.getLong(KEY_LOCAL_ID, -1L)
        set(value) { prefs.edit().putLong(KEY_LOCAL_ID, value).apply() }

    var localName: String?
        get() = prefs.getString(KEY_LOCAL_NAME, null)
        set(value) { prefs.edit().putString(KEY_LOCAL_NAME, value).apply() }

    var tvId: Long
        get() = prefs.getLong(KEY_TV_ID, -1L)
        set(value) { prefs.edit().putLong(KEY_TV_ID, value).apply() }

    /** -1L means "Automático según horario", any other value means manual playlist id. */
    var selectedPlaylistId: Long
        get() = prefs.getLong(KEY_SELECTED_PLAYLIST, -2L)
        set(value) { prefs.edit().putLong(KEY_SELECTED_PLAYLIST, value).apply() }

    fun isAutoMode(): Boolean = selectedPlaylistId == -1L
    fun hasPlaylistChoice(): Boolean = selectedPlaylistId != -2L

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_LOCAL_ID)
            .remove(KEY_LOCAL_NAME)
            .remove(KEY_TV_ID)
            .remove(KEY_SELECTED_PLAYLIST)
            .apply()
    }

    companion object {
        private const val KEY_SERVER = "server_url"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_LOCAL_ID = "local_id"
        private const val KEY_LOCAL_NAME = "local_name"
        private const val KEY_TV_ID = "tv_id"
        private const val KEY_SELECTED_PLAYLIST = "selected_playlist"
    }
}
