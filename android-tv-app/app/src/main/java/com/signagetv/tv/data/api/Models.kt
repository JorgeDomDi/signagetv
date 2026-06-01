package com.signagetv.tv.data.api

import com.google.gson.annotations.SerializedName

// ===== Auth =====

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val local: LocalDto
)

data class LocalDto(
    val id: Long,
    val nombre: String,
    val username: String? = null
)

// ===== TV registration =====

data class RegisterTvRequest(
    @SerializedName("device_id") val deviceId: String,
    val nombre: String
)

data class TvDto(
    val id: Long,
    val nombre: String,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("current_playlist_id") val currentPlaylistId: Long? = null
)

data class AssignPlaylistRequest(
    // El backend usa snake_case en JSON público: espera "playlist_id".
    @SerializedName("playlist_id") val playlistId: Long?
)

// ===== Playlist / Media =====

data class PlaylistDto(
    val id: Long,
    val nombre: String,
    val transicion: String = "FADE",
    @SerializedName("default_image_seconds") val defaultImageSeconds: Int = 8,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val items: List<PlaylistItemDto> = emptyList()
)

data class PlaylistItemDto(
    val id: Long,
    val position: Int,
    @SerializedName("duration_seconds") val durationSeconds: Int? = null,
    @SerializedName("repeat_count") val repeatCount: Int? = null,
    val media: MediaItemDto
)

data class MediaItemDto(
    val id: Long,
    val filename: String,
    /** Absolute URL served by backend (resolved server-side). */
    val url: String,
    /** "IMAGE" or "VIDEO" */
    val type: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int? = null
)

/**
 * Returned by GET /tv/playlist/current.
 *
 * IMPORTANT: The backend (TvPlaybackController) returns the {@link PlaylistDto}
 * directly (or HTTP 200 with body `null` when no playlist is active). We keep a
 * thin wrapper here so the player code can call {@code res.playlist} without
 * worrying about nullability — the repository layer adapts the raw API response
 * into this wrapper.
 */
data class CurrentPlaylistResponse(
    val playlist: PlaylistDto?,
    /** Reservado para futuras extensiones (origen: MANUAL / SCHEDULE / NONE). */
    val source: String? = null
)
