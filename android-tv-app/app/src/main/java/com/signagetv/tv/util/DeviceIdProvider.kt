package com.signagetv.tv.util

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Identidad de esta TV frente al servidor.
 *
 * Se deriva del ANDROID_ID del aparato, que **sobrevive a desinstalar y volver a
 * instalar la app y a borrar los datos**. Antes se usaba un UUID al azar guardado
 * en preferencias: cada instalacion limpia generaba una identidad nueva, la TV
 * aparecia como una pantalla distinta en el panel y perdia la playlist que tenia
 * asignada. Asi se juntaron 16 filas para 6 televisores.
 *
 * La migracion ocurre una sola vez: si la TV ya venia con un UUID viejo, pasa al
 * id estable la primera vez que arranca esta version y de ahi no cambia nunca mas.
 */
object DeviceIdProvider {

    private const val PREFS = "signagetv_device"
    private const val KEY_DEVICE_ID = "device_id"
    private const val STABLE_PREFIX = "android-"

    /** ANDROID_ID defectuoso que compartian muchos aparatos viejos: no sirve como identidad. */
    private const val BROKEN_ANDROID_ID = "9774d56d682e549c"

    fun get(context: Context): String {
        val ctx = context.applicationContext
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_DEVICE_ID, null)

        // Ya tiene identidad estable: no se toca nunca mas.
        if (stored != null && stored.startsWith(STABLE_PREFIX)) return stored

        val stable = stableId(ctx)

        if (stable == null) {
            // El aparato no da un ANDROID_ID utilizable: seguimos con el UUID de siempre.
            val fallback = stored ?: UUID.randomUUID().toString()
            if (stored == null) {
                prefs.edit().putString(KEY_DEVICE_ID, fallback).apply()
                Logger.w("Sin ANDROID_ID utilizable; uso identidad aleatoria")
            }
            return fallback
        }

        if (stored != null) {
            Logger.i("Migrando identidad de la TV a una estable del aparato")
        }
        prefs.edit().putString(KEY_DEVICE_ID, stable).apply()
        return stable
    }

    private fun stableId(ctx: Context): String? {
        val androidId = try {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (t: Throwable) {
            Logger.w("No se pudo leer ANDROID_ID: ${t.message}")
            null
        }
        if (androidId.isNullOrBlank() || androidId == BROKEN_ANDROID_ID) return null
        return STABLE_PREFIX + androidId
    }
}
