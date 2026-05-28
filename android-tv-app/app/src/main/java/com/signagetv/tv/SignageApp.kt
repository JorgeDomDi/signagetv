package com.signagetv.tv

import android.app.Application
import com.signagetv.tv.data.api.ApiClient
import com.signagetv.tv.data.prefs.SignagePrefs
import com.signagetv.tv.data.repository.SignageRepository
import com.signagetv.tv.util.DeviceIdProvider

/**
 * Application + tiny service locator. Avoids pulling in a DI framework for a 3-screen app.
 */
class SignageApp : Application() {

    lateinit var prefs: SignagePrefs
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var repository: SignageRepository
        private set
    lateinit var deviceId: String
        private set

    override fun onCreate() {
        super.onCreate()
        deviceId = DeviceIdProvider.get(this)
        prefs = SignagePrefs(this)
        apiClient = ApiClient(prefs) { deviceId }
        repository = SignageRepository(this, apiClient, prefs)
    }

    companion object {
        fun get(context: android.content.Context): SignageApp =
            context.applicationContext as SignageApp
    }
}
