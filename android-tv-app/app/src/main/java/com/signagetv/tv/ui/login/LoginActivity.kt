package com.signagetv.tv.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.signagetv.tv.R
import com.signagetv.tv.SignageApp
import com.signagetv.tv.databinding.ActivityLoginBinding
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

        // Pre-fill last server URL if present
        binding.serverUrlInput.setText(app.prefs.serverUrl ?: "")

        // If we already have a valid session + playlist choice, skip straight to player.
        if (!app.prefs.token.isNullOrBlank() && app.prefs.hasPlaylistChoice()) {
            startActivity(Intent(this, PlaylistSelectActivity::class.java))
            finish()
            return
        }

        binding.loginButton.setOnClickListener { performLogin() }

        // Focus the empty field that comes first
        when {
            binding.serverUrlInput.text.isNullOrBlank() -> binding.serverUrlInput.requestFocus()
            else -> binding.usernameInput.requestFocus()
        }
    }

    private fun performLogin() {
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
                startActivity(Intent(this@LoginActivity, PlaylistSelectActivity::class.java))
                finish()
            } catch (t: Throwable) {
                Logger.e("Login failed", t)
                showError(getString(R.string.error_login, t.message ?: "unknown"))
                setLoading(false)
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
