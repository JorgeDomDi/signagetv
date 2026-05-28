package com.signagetv.tv.util

import android.content.Context
import java.util.UUID

/**
 * Generates a stable UUID for this TV on first launch and persists it.
 * Used as X-Device-Id header so the backend can identify the device across
 * reinstalls of the user/password but the same physical TV will get the same id
 * for the life of the app data.
 */
object DeviceIdProvider {
    private const val PREFS = "signagetv_device"
    private const val KEY_DEVICE_ID = "device_id"

    fun get(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, fresh).apply()
        return fresh
    }
}
