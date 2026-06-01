package com.signagetv.tv.data.ws

import com.signagetv.tv.util.Logger
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal STOMP-over-WebSocket client tailored to what SignageTV needs:
 *  - CONNECT / CONNECTED handshake
 *  - SUBSCRIBE to topics
 *  - Receive MESSAGE frames and dispatch payload
 *  - Auto-reconnect with exponential backoff
 *
 * Doesn't speak SockJS - it expects the raw STOMP-over-WebSocket endpoint
 * (Spring's `withSockJS()` also exposes a plain WS fallback at the same path
 *  with `/websocket` suffix). We default to "{base}/ws/websocket" if SockJS is
 * enabled server-side; LoginActivity persists the base URL.
 */
class StompClient(
    private val okHttp: OkHttpClient,
    private val wsUrl: String,
    private val token: String?,
    private val scope: CoroutineScope
) {
    interface Listener {
        fun onConnected() {}
        fun onMessage(destination: String, body: String) {}
        fun onDisconnected(reason: String?) {}
    }

    private var listener: Listener? = null
    private var webSocket: WebSocket? = null
    private val subId = AtomicInteger(0)
    private val pendingSubs = mutableMapOf<String, String>()
    private val activeSubs = mutableMapOf<String, String>()
    private var reconnectJob: Job? = null
    private var stopped = false
    private var backoffMs = INITIAL_BACKOFF_MS

    fun setListener(l: Listener) { listener = l }

    fun connect() {
        stopped = false
        openSocket()
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        try {
            webSocket?.send("DISCONNECT\n\n" + NUL)
        } catch (_: Throwable) {}
        webSocket?.close(1000, "client stop")
        webSocket = null
        activeSubs.clear()
    }

    fun subscribe(destination: String) {
        val ws = webSocket
        if (ws == null || !activeSubs.containsKey(destination)) {
            val id = "sub-" + subId.incrementAndGet()
            pendingSubs[destination] = id
            if (ws != null) {
                sendSubscribe(ws, destination, id)
            }
        }
    }

    private fun sendSubscribe(ws: WebSocket, destination: String, id: String) {
        val frame = buildString {
            append("SUBSCRIBE\n")
            append("id:").append(id).append('\n')
            append("destination:").append(destination).append('\n')
            append('\n')
            append(NUL)
        }
        if (ws.send(frame)) {
            activeSubs[destination] = id
            pendingSubs.remove(destination)
            Logger.i("STOMP SUBSCRIBE " + destination + " (" + id + ")")
        }
    }

    private fun openSocket() {
        if (stopped) return
        val request = Request.Builder()
            .url(wsUrl)
            .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
        Logger.i("STOMP connecting to " + wsUrl)
        webSocket = okHttp.newWebSocket(request, socketListener)
    }

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            val connect = buildString {
                append("CONNECT\n")
                append("accept-version:1.2\n")
                append("host:signagetv\n")
                token?.let { append("Authorization:Bearer ").append(it).append('\n') }
                append("heart-beat:10000,10000\n")
                append('\n')
                append(NUL)
            }
            ws.send(connect)
        }

        override fun onMessage(ws: WebSocket, text: String) {
            handleFrame(ws, text)
        }

        override fun onMessage(ws: WebSocket, bytes: okio.ByteString) {
            handleFrame(ws, bytes.utf8())
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Logger.w("STOMP onFailure: " + t.message)
            activeSubs.clear()
            listener?.onDisconnected(t.message)
            scheduleReconnect()
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Logger.i("STOMP onClosing " + code + " " + reason)
            ws.close(code, reason)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Logger.i("STOMP onClosed " + code + " " + reason)
            activeSubs.clear()
            listener?.onDisconnected(reason)
            if (!stopped) scheduleReconnect()
        }
    }

    private fun handleFrame(ws: WebSocket, raw: String) {
        if (raw.isEmpty() || raw == "\n") return
        val end = raw.indexOf(NUL_CHAR).let { if (it < 0) raw.length else it }
        val frame = raw.substring(0, end)
        val firstLine = frame.substringBefore('\n')
        when (firstLine.trim()) {
            "CONNECTED" -> {
                Logger.i("STOMP CONNECTED")
                backoffMs = INITIAL_BACKOFF_MS
                val toResub = activeSubs.keys.toList() + pendingSubs.keys.toList()
                activeSubs.clear()
                pendingSubs.clear()
                toResub.distinct().forEach { dest ->
                    val id = "sub-" + subId.incrementAndGet()
                    sendSubscribe(ws, dest, id)
                }
                listener?.onConnected()
            }
            "MESSAGE" -> {
                val headerEnd = frame.indexOf("\n\n")
                if (headerEnd < 0) return
                val headers = parseHeaders(frame.substring(0, headerEnd))
                val body = frame.substring(headerEnd + 2)
                val dest = headers["destination"] ?: return
                listener?.onMessage(dest, body)
            }
            "ERROR" -> {
                Logger.w("STOMP ERROR frame: " + frame)
            }
        }
    }

    private fun parseHeaders(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        raw.split('\n').drop(1).forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
        }
        return map
    }

    private fun scheduleReconnect() {
        if (stopped) return
        reconnectJob?.cancel()
        val wait = backoffMs
        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        reconnectJob = scope.launch {
            Logger.i("STOMP reconnect in " + wait + "ms")
            delay(wait)
            openSocket()
        }
    }

    companion object {
        // STOMP frame terminator is the NUL byte (U+0000).
        // Stored in non-const fields so we never embed a literal NUL in source.
        private val NUL_CHAR: Char = Char(0)
        private val NUL: String = NUL_CHAR.toString()
        private const val INITIAL_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 60_000L
    }
}
