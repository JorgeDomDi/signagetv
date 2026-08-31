package com.signagetv.tv.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.signagetv.tv.BuildConfig
import com.signagetv.tv.R
import com.signagetv.tv.SignageApp
import com.signagetv.tv.databinding.ActivityLoginBinding
import com.signagetv.tv.ui.player.PlayerActivity
import com.signagetv.tv.ui.select.PlaylistSelectActivity
import com.signagetv.tv.util.Logger
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { SignageApp.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Datos de fabrica (BuildConfig): una TV recien instalada ya viene con
        // el servidor y la cuenta de la tienda cargados. Siguen siendo editables.
        binding.serverUrlInput.setText(app.prefs.serverUrl ?: BuildConfig.DEFAULT_SERVER_URL)
        binding.usernameInput.setText(BuildConfig.DEFAULT_USERNAME)
        binding.passwordInput.setText(BuildConfig.DEFAULT_PASSWORD)

        // If we already have a valid session + playlist choice, skip straight to player.
        if (!app.prefs.token.isNullOrBlank() && app.prefs.hasPlaylistChoice()) {
            startActivity(Intent(this, PlaylistSelectActivity::class.java))
            finish()
            return
        }

        binding.loginButton.setOnClickListener { performLogin() }

        // Arranque desatendido: si nunca se inicio sesion en esta TV y hay
        // credenciales de fabrica, entramos solos y vamos directo al reproductor
        // en modo "Automatico segun horario". Todo lo demas se maneja desde el
        // panel web. Si falla (cuenta cambiada, sin red), quedan los campos
        // cargados y el mensaje de error para hacerlo a mano.
        if (shouldAutoLogin()) {
            app.prefs.autoLoginDone = true
            binding.errorView.visibility = View.GONE
            performLogin(auto = true)
            return
        }

        // Focus the empty field that comes first
        when {
            binding.serverUrlInput.text.isNullOrBlank() -> binding.serverUrlInput.requestFocus()
            else -> binding.usernameInput.requestFocus()
        }
    }

    private fun shouldAutoLogin(): Boolean =
        BuildConfig.AUTO_START &&
            !app.prefs.autoLoginDone &&
            app.prefs.token.isNullOrBlank() &&
            BuildConfig.DEFAULT_USERNAME.isNotBlank() &&
            BuildConfig.DEFAULT_PASSWORD.isNotBlank()

    private fun performLogin(auto: Boolean = false) {
        val server = binding.serverUrlInput.text.toString().trim()
        val user = binding.usernameInput.text.toString().trim()
        val pwd = binding.passwordInput.text.toString()

        if (server.isEmpty() || user.isEmpty() || pwd.isEmpty()) {
            showError(getString(R.string.error_empty_fields))
            return
        }
        setLoading(true)

        lifecycleScope.launch {
            try {
                val res = app.repository.login(server, user, pwd)
                Logger.i("Login OK local=${res.local.id} (${res.local.nombre})")
                val tvName = "TV-" + Build.MODEL.ifBlank { "Android" }
                val tv = app.repository.registerTv(app.deviceId, tvName)
                Logger.i("TV registered id=${tv.id}")

                if (auto) {
                    // Modo desatendido: elegimos "Automatico segun horario" y vamos
                    // derecho a reproducir. Desde el panel se decide que se ve.
                    app.prefs.selectedPlaylistId = -1L
                    runCatching { app.repository.assignPlaylist(tv.id, null) }
                        .onFailure { Logger.w("assignPlaylist failed: ${it.message}") }
                    startActivity(Intent(this@LoginActivity, PlayerActivity::class.java))
                } else {
                    startActivity(Intent(this@LoginActivity, PlaylistSelectActivity::class.java))
                }
                finish()
            } catch (t: Throwable) {
                Logger.e("Login failed", t)
                showError(
                    if (auto) getString(R.string.login_auto_failed, t.message ?: "sin conexión")
                    else getString(R.string.error_login, t.message ?: "unknown")
                )
                setLoading(false)
                if (auto) binding.loginButton.requestFocus()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !loading
        binding.usernameInput.isEnabled = !loading
        binding.passwordInput.isEnabled = !loading
        binding.serverUrlInput.isEnabled = !loading
        if (loading) binding.errorView.visibility = View.GONE
    }

    private fun showError(msg: String) {
        binding.errorView.text = msg
        binding.errorView.visibility = View.VISIBLE
    }
}
