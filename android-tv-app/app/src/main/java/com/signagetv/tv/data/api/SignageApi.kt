package com.signagetv.tv.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SignageApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/v1/playlists")
    suspend fun listPlaylists(): List<PlaylistDto>

    @POST("api/v1/tvs/register")
    suspend fun registerTv(@Body body: RegisterTvRequest): TvDto

    @PUT("api/v1/tvs/{id}/playlist")
    suspend fun assignPlaylist(
        @Path("id") tvId: Long,
        @Body body: AssignPlaylistRequest
    ): TvDto

    /**
     * Backend devuelve `PlaylistDto` directamente (o body `null` si no hay nada que
     * reproducir). El Repository envuelve el resultado en `CurrentPlaylistResponse`
     * para que la UI siga accediendo a `.playlist`.
     */
    @GET("api/v1/tv/playlist/current")
    suspend fun getCurrentPlaylist(): PlaylistDto?
}
