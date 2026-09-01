package com.signagetv.tv.data.api

import retrofit2.Response
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
     * Devolvemos `Response<>` en vez del cuerpo pelado para poder distinguir el
     * caso "no hay nada que reproducir" sin que Retrofit se queje. Con el tipo
     * pelado, un 204 hacia estallar la app con "response body type was declared
     * as non-null" y un 200 con cuerpo vacio reventaba en Gson.
     */
    @GET("api/v1/tv/playlist/current")
    suspend fun getCurrentPlaylist(): Response<PlaylistDto>
}
