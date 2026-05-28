package com.signagetv.tv.ui.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.gson.JsonParser
import com.signagetv.tv.R
import com.signagetv.tv.SignageApp
import com.signagetv.tv.data.api.PlaylistDto
import com.signagetv.tv.data.api.PlaylistItemDto
import com.signagetv.tv.data.ws.StompClient
import com.signagetv.tv.databinding.ActivityPlayerBinding
import com.signagetv.tv.ui.select.PlaylistSelectActivity
import com.signagetv.tv.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fullscreen content player.
 *  - Cycles through the playlist items returned by GET /tv/playlist/current.
 *  - Images: cross-fade / slide / zoom (per playlist.transicion).
 *  - Videos: ExoPlayer, advance on completion.
 *  - WebSocket subscription to /topic/local/{localId}/playlists to refresh on changes.
 *  - 60 s polling fallback.
 *  - Long-press BACK (3 s) returns to the playlist selector.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val app by lazy { SignageApp.get(this) }
    private val main = Handler(Looper.getMainLooper())

    private var exoPlayer: ExoPlayer? = null
    private var stomp: StompClient? = null
    private var pollJob: Job? = null
    private var loopJob: Job? = null

    /** Current playlist actively being shown. */
    private var current: PlaylistDto? = null

    /** Next playlist queued (will swap when current item ends). */
    @Volatile private var pendingPlaylist: PlaylistDto? = null
    @Volatile private var pendingRefresh: Boolean = false

    private var imageFrontVisible = false  // false -> imageBack is the "front"
    private var backHoldStart: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goImmersive()

        exoPlayer = ExoPlayer.Builder(this).build().also { p ->
            binding.playerView.player = p
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) advanceItem()
                }
                override fun onPlayerError(error: PlaybackException) {
                    Logger.e("ExoPlayer error", error)
                    advanceItem()
                }
            })
        }

        fetchAndStart(initial = true)
        startWebSocket()
        startPolling()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    private fun goImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    // ===== Network: playlist fetch =====

    private fun fetchAndStart(initial: Boolean) {
        showLoading(initial)
        lifecycleScope.launch {
            try {
                val res = app.repository.getCurrentPlaylist()
                val pl = res.playlist
                if (pl == null || pl.items.isEmpty()) {
                    showMessage(getString(R.string.player_no_content))
                    current = null
                    return@launch
                }
                showLoading(false)
                if (current == null) {
                    current = pl
                    startLoop()
                } else {
                    pendingPlaylist = pl
                    Logger.i("New playlist queued; will swap at item boundary")
                }
            } catch (t: Throwable) {
                Logger.e("fetchAndStart failed", t)
                if (initial) showMessage("Error: ${t.message}")
            }
        }
    }

    // ===== Loop =====

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = lifecycleScope.launch {
            while (true) {
                val playlist = current ?: return@launch
                val items = playlist.items
                if (items.isEmpty()) return@launch
                for (item in items.sortedBy { it.position }) {
                    if (consumePending()) break  // restart loop with new playlist
                    showItem(playlist, item)
                    // showItem suspends until item duration is up (or video ends)
                }
            }
        }
    }

    private fun consumePending(): Boolean {
        val next = pendingPlaylist
        if (next != null) {
            pendingPlaylist = null
            current = next
            Logger.i("Swapping to queued playlist '${next.nombre}'")
            return true
        }
        if (pendingRefresh) {
            pendingRefresh = false
            fetchAndStart(initial = false)
        }
        return false
    }

    private suspend fun showItem(playlist: PlaylistDto, item: PlaylistItemDto) {
        val media = item.media
        try {
            when (media.type.uppercase()) {
                "IMAGE" -> showImage(playlist, item)
                "VIDEO" -> showVideo(item)
                else -> Logger.w("Unknown media type ${media.type}, skipping")
            }
        } catch (t: Throwable) {
            Logger.e("Failed to show item ${media.id} (${media.filename})", t)
            // skip with small delay so we don't tight-loop on persistent errors
            delay(500)
        }
    }

    private suspend fun showImage(playlist: PlaylistDto, item: PlaylistItemDto) {
        binding.playerView.visibility = View.GONE
        binding.imageFront.visibility = View.VISIBLE
        binding.imageBack.visibility = View.VISIBLE
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()

        val file = ensureCached(item.media.url)
        val request = ImageRequest.Builder(this)
            .data(file ?: item.media.url)
            .allowHardware(true)
            .build()
        val result = Coil.imageLoader(this).execute(request)
        val drawable = (result as? SuccessResult)?.drawable
            ?: throw IllegalStateException("Coil returned no drawable for ${item.media.url}")

        val target = if (imageFrontVisible) binding.imageBack else binding.imageFront
        val current = if (imageFrontVisible) binding.imageFront else binding.imageBack
        target.setImageDrawable(drawable)
        animateTransition(playlist.transicion, fromView = current, toView = target)
        imageFrontVisible = !imageFrontVisible

        val secs = item.durationSeconds ?: playlist.defaultImageSeconds.coerceAtLeast(2)
        delay(secs * 1000L)
    }

    private suspend fun showVideo(item: PlaylistItemDto) {
        binding.imageFront.visibility = View.INVISIBLE
        binding.imageBack.visibility = View.INVISIBLE
        binding.playerView.visibility = View.VISIBLE

        val file = ensureCached(item.media.url)
        val uri = file?.toURI()?.toString() ?: item.media.url
        val player = exoPlayer ?: return
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true

        // Wait for completion via the player listener -> advanceItem fires.
        videoCompletionGate()
    }

    /** Suspends until the player reaches STATE_ENDED or hits an error (both call advanceItem()). */
    private suspend fun videoCompletionGate() {
        val player = exoPlayer ?: return
        // Poll once a second; simpler than a flow setup and fine for content of seconds/minutes.
        while (true) {
            if (player.playbackState == Player.STATE_ENDED) return
            if (advancePending) { advancePending = false; return }
            delay(500)
        }
    }

    @Volatile private var advancePending = false
    private fun advanceItem() {
        advancePending = true
    }

    private fun animateTransition(kind: String, fromView: View, toView: View) {
        toView.alpha = 0f
        toView.translationX = 0f
        toView.scaleX = 1f
        toView.scaleY = 1f
        when (kind.uppercase()) {
            "FADE" -> {
                toView.animate().alpha(1f).setDuration(800).start()
                fromView.animate().alpha(0f).setDuration(800).start()
            }
            "SLIDE" -> {
                toView.translationX = toView.width.toFloat()
                toView.alpha = 1f
                toView.animate().translationX(0f).setDuration(700).start()
                fromView.animate().translationX(-fromView.width.toFloat()).setDuration(700)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            fromView.translationX = 0f
                            fromView.alpha = 0f
                        }
                    }).start()
            }
            "ZOOM" -> {
                toView.scaleX = 1.15f; toView.scaleY = 1.15f
                toView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900).start()
                fromView.animate().alpha(0f).setDuration(900).start()
            }
            "NONE" -> {
                toView.alpha = 1f
                fromView.alpha = 0f
            }
            else -> {
                toView.animate().alpha(1f).setDuration(500).start()
                fromView.animate().alpha(0f).setDuration(500).start()
            }
        }
    }

    private suspend fun ensureCached(url: String): File? = try {
        app.repository.ensureMediaCached(url)
    } catch (t: Throwable) {
        Logger.w("Cache miss for $url, streaming instead: ${t.message}")
        null
    }

    // ===== WebSocket =====

    private fun startWebSocket() {
        val base = app.prefs.serverUrl?.removeSuffix("/") ?: return
        val wsBase = base.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        // SignageTV backend mounts STOMP at /ws (raw WS endpoint when SockJS is disabled,
        // or /ws/websocket when SockJS is enabled). We try the websocket suffix first.
        val wsUrl = "$wsBase/ws/websocket"
        val token = app.prefs.token
        val client = StompClient(app.apiClient.okHttpClient, wsUrl, token, lifecycleScope)
        client.setListener(object : StompClient.Listener {
            override fun onConnected() {
                val localId = app.prefs.localId
                if (localId > 0) client.subscribe("/topic/local/$localId/playlists")
                val tvId = app.prefs.tvId
                if (tvId > 0) client.subscribe("/topic/tv/$tvId/command")
            }
            override fun onMessage(destination: String, body: String) {
                Logger.i("WS message $destination -> $body")
                // Anything received on these topics triggers a refresh attempt.
                val shouldRefresh = try {
                    val json = JsonParser.parseString(body).asJsonObject
                    val type = json.get("type")?.asString?.uppercase()
                    type == null || type == "REFRESH" || type == "PLAYLIST_CHANGED" ||
                        type == "SCHEDULE_CHANGED"
                } catch (_: Throwable) { true }
                if (shouldRefresh) {
                    pendingRefresh = true
                    fetchAndStart(initial = false)
                }
            }
            override fun onDisconnected(reason: String?) {
                Logger.w("WS disconnected: $reason")
            }
        })
        client.connect()
        stomp = client
    }

    // ===== Polling fallback =====

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (true) {
                delay(60_000)
                Logger.d("Polling /tv/playlist/current")
                fetchAndStart(initial = false)
            }
        }
    }

    // ===== UI =====

    private fun showLoading(visible: Boolean) {
        binding.loadingPanel.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) binding.errorView.visibility = View.GONE
    }

    private fun showMessage(msg: String) {
        binding.loadingPanel.visibility = View.GONE
        binding.errorView.text = msg
        binding.errorView.visibility = View.VISIBLE
    }

    // ===== Back / menu handling =====

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (backHoldStart == 0L) {
                    backHoldStart = System.currentTimeMillis()
                    binding.backHint.visibility = View.VISIBLE
                    main.postDelayed(hideHint, 3500)
                }
                if (System.currentTimeMillis() - backHoldStart >= 3000L) {
                    returnToSelect()
                    return true
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                returnToSelect()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val held = System.currentTimeMillis() - backHoldStart
            backHoldStart = 0L
            main.removeCallbacks(hideHint)
            if (held >= 3000L) {
                returnToSelect()
                return true
            }
            // brief press shows hint only
            binding.backHint.visibility = View.VISIBLE
            main.postDelayed(hideHint, 2500)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private val hideHint = Runnable { binding.backHint.visibility = View.GONE }

    private fun returnToSelect() {
        startActivity(Intent(this, PlaylistSelectActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel()
        pollJob?.cancel()
        stomp?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
