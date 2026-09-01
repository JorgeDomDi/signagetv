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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fullscreen content player.
 *  - Cycles through the playlist items returned by GET /tv/playlist/current.
 *  - Images: cross-fade / slide / zoom (per playlist.transicion).
 *  - Videos: ExoPlayer, advance on completion.
 *  - WebSocket subscription to /topic/local/{localId}/playlists to refresh on changes.
 *  - 60 s polling fallback.
 *  - Background music: a SECOND, independent ExoPlayer loops the playlist's audio
 *    tracks (REPEAT_MODE_ALL) while images/videos rotate on their own. When the
 *    playlist has music, videos are played muted so the music always has priority.
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

    /** Reproductor dedicado a la musica de fondo. Corre en paralelo al visual. */
    private var audioPlayer: ExoPlayer? = null
    private var audioJob: Job? = null

    /**
     * Firma de la lista de pistas que esta sonando ahora mismo (ids en orden).
     * Sirve para NO reiniciar la musica cuando la playlist se recarga pero la
     * lista de pistas es la misma.
     */
    private var currentAudioKey: String? = null

    /** Se incrementa en cada cambio de musica; un job viejo que despierte se descarta. */
    private var audioGeneration = 0

    /** true si la playlist activa tiene musica -> los videos se reproducen muteados. */
    @Volatile private var musicHasPriority = false

    /** Current playlist actively being shown. */
    private var current: PlaylistDto? = null

    /**
     * Huella del contenido que se esta reproduciendo ahora mismo. Se compara con
     * la del servidor para saber si algo cambio DE VERDAD.
     *
     * No usamos `updated_at`: la columna en MySQL tiene resolucion de 1 segundo,
     * asi que dos guardados seguidos (o un guardado en el mismo segundo en que la
     * TV consulto) daban la misma marca y el cambio se perdia en silencio.
     */
    private var currentFingerprint: String? = null

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

        audioPlayer = ExoPlayer.Builder(this).build().also { a ->
            a.repeatMode = Player.REPEAT_MODE_ALL
            a.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Una pista rota no debe cortar la musica: saltamos a la siguiente.
                    Logger.e("Audio player error, skipping track", error)
                    a.seekToNextMediaItem()
                    a.prepare()
                    a.playWhenReady = true
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
                    currentFingerprint = null
                    return@launch
                }
                showLoading(false)
                val fp = pl.fingerprint()
                if (current == null || fp != currentFingerprint) {
                    Logger.i("Contenido nuevo -> aplicando al instante: '${pl.nombre}'")
                    applyPlaylist(pl, fp)
                } else {
                    Logger.d("Sin cambios; sigue el ciclo actual")
                }
            } catch (t: Throwable) {
                // En la pantalla de un local no se muestra un error tecnico: no le
                // sirve a nadie y queda pesimo a la vista del publico. Queda en el
                // log (adb logcat) y se reintenta en el proximo poll. Si ya habia
                // algo reproduciendose, ni se toca.
                Logger.e("No se pudo traer la playlist", t)
                if (current == null) showMessage(getString(R.string.player_no_content))
            }
        }
    }

    // ===== Loop =====

    /**
     * Huella del contenido: todo lo que, si cambia, obliga a repintar la pantalla.
     * Si dos respuestas del servidor dan la misma huella, no hay nada que hacer.
     */
    private fun PlaylistDto.fingerprint(): String = buildString {
        append(id).append('|').append(transicion).append('|').append(defaultImageSeconds).append('|')
        items.sortedBy { it.position }.forEach { i ->
            append(i.media.id).append(':')
                .append(i.durationSeconds ?: -1).append(':')
                .append(i.repeatCount ?: 1).append(',')
        }
        append('#')
        audioItems.orEmpty().sortedBy { it.position }.forEach { a ->
            append(a.media.id).append(',')
        }
    }

    /**
     * Aplica una playlist AL INSTANTE.
     *
     * Cancelar `loopJob` corta la espera de la imagen en curso o del video que se
     * este reproduciendo, asi que el contenido nuevo entra en el momento y no al
     * terminar el item actual. Tambien desatasca el ciclo si se habia quedado
     * clavado esperando un video que nunca termino.
     *
     * La musica NO se reinicia si las pistas son las mismas: de eso se encarga
     * [syncBackgroundAudio], que compara la lista antes de tocar el reproductor.
     */
    private fun applyPlaylist(pl: PlaylistDto, fingerprint: String = pl.fingerprint()) {
        current = pl
        currentFingerprint = fingerprint
        binding.errorView.visibility = View.GONE
        syncBackgroundAudio(pl)
        startLoop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = lifecycleScope.launch {
            while (isActive) {
                val playlist = current ?: return@launch
                val items = playlist.items.sortedBy { it.position }
                if (items.isEmpty()) return@launch
                for (item in items) {
                    if (!isActive) return@launch
                    showItem(playlist, item)
                    // showItem suspende hasta que se cumple la duracion (o termina el video)
                }
            }
        }
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

        // Repeticiones: encolamos el mismo video N veces en el propio reproductor.
        // ExoPlayer reproduce la cola de forma continua (sin corte negro) y dispara
        // STATE_ENDED recien al terminar la ultima repeticion -> avanza al siguiente item.
        val repeat = (item.repeatCount ?: 1).coerceIn(1, 100)
        val mediaItems = (1..repeat).map { MediaItem.fromUri(uri) }
        advancePending = false
        // La musica de fondo manda: si la playlist tiene pistas, el video va mudo.
        player.volume = if (musicHasPriority) 0f else 1f
        player.setMediaItems(mediaItems)
        player.prepare()
        player.playWhenReady = true

        // Wait for completion via the player listener -> advanceItem fires.
        videoCompletionGate()
    }

    /**
     * Espera a que el video termine (STATE_ENDED) o falle.
     *
     * Ademas vigila que no se quede colgado: si ExoPlayer se queda en IDLE tras un
     * fallo silencioso, o si la posicion deja de avanzar porque el buffer se trabo,
     * pasamos al siguiente item. Sin esto el ciclo se clavaba para siempre en un
     * video roto y la TV dejaba de tomar los cambios de la playlist.
     */
    private suspend fun videoCompletionGate() {
        val player = exoPlayer ?: return
        var lastPosition = -1L
        var stalledMs = 0L
        var idleMs = 0L

        while (true) {
            if (player.playbackState == Player.STATE_ENDED) return
            if (advancePending) { advancePending = false; return }
            delay(GATE_POLL_MS)

            if (player.playbackState == Player.STATE_IDLE) {
                idleMs += GATE_POLL_MS
                if (idleMs >= IDLE_GIVE_UP_MS) {
                    Logger.w("Video sin arrancar tras ${IDLE_GIVE_UP_MS}ms; salto al siguiente item")
                    return
                }
            } else {
                idleMs = 0L
            }

            val pos = player.currentPosition
            if (pos == lastPosition && player.playWhenReady) {
                stalledMs += GATE_POLL_MS
                if (stalledMs >= STALL_GIVE_UP_MS) {
                    Logger.w("Video trabado ${STALL_GIVE_UP_MS}ms sin avanzar; salto al siguiente item")
                    return
                }
            } else {
                stalledMs = 0L
                lastPosition = pos
            }
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

    // ===== Musica de fondo =====

    /**
     * Pone en marcha (o deja como esta) la musica de fondo de [playlist].
     *
     * Si la lista de pistas es identica a la que ya esta sonando no se toca nada,
     * asi el poll de 60 s, un REFRESH del WebSocket o un cambio que solo afecta a
     * las imagenes no cortan la musica a la mitad.
     *
     * Las pistas se descargan a cache una por una: la primera arranca en cuanto
     * esta lista y el resto se van encolando a medida que bajan, para no quedarse
     * varios minutos en silencio esperando un compilado pesado.
     */
    private fun syncBackgroundAudio(playlist: PlaylistDto) {
        val tracks = playlist.audioItems.orEmpty().sortedBy { it.position }
        val key = tracks.joinToString(",") { it.media.id.toString() }

        if (key == currentAudioKey) {
            Logger.d("Background audio unchanged; keeping it playing")
            return
        }
        currentAudioKey = key
        musicHasPriority = tracks.isNotEmpty()

        audioJob?.cancel()
        val generation = ++audioGeneration
        val player = audioPlayer ?: return
        player.stop()
        player.clearMediaItems()

        if (tracks.isEmpty()) {
            Logger.i("Playlist '${playlist.nombre}' has no background music")
            return
        }

        Logger.i("Background music: ${tracks.size} track(s) on repeat")
        audioJob = lifecycleScope.launch {
            var started = false
            for (track in tracks) {
                val cached = ensureCached(track.media.url) ?: continue
                if (generation != audioGeneration) return@launch  // la musica ya cambio
                val mediaItem = MediaItem.fromUri(cached.toURI().toString())
                player.addMediaItem(mediaItem)
                if (!started) {
                    started = true
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    player.volume = 1f
                    player.prepare()
                    player.playWhenReady = true
                }
            }
            if (!started && generation == audioGeneration) {
                Logger.w("No background track could be cached; playing without music")
                musicHasPriority = false
            }
        }
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
                if (shouldRefresh) fetchAndStart(initial = false)
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
                delay(POLL_INTERVAL_MS)
                Logger.d("Poll de respaldo /tv/playlist/current")
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

    companion object {
        /** Cada cuanto se revisa el estado del video en curso. */
        private const val GATE_POLL_MS = 500L
        /** Si ExoPlayer sigue en IDLE tanto tiempo, damos el video por perdido. */
        private const val IDLE_GIVE_UP_MS = 10_000L
        /** Si la posicion no avanza tanto tiempo con el player en marcha, idem. */
        private const val STALL_GIVE_UP_MS = 60_000L
        /**
         * Respaldo por si el WebSocket se cayo. El camino normal es el push, que es
         * instantaneo; esto solo acota cuanto puede tardar una TV desconectada.
         */
        private const val POLL_INTERVAL_MS = 20_000L
    }

    private fun returnToSelect() {
        startActivity(Intent(this, PlaylistSelectActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel()
        pollJob?.cancel()
        audioJob?.cancel()
        stomp?.stop()
        exoPlayer?.release()
        exoPlayer = null
        audioPlayer?.release()
        audioPlayer = null
    }
}
