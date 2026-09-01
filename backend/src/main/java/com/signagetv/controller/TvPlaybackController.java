package com.signagetv.controller;

import com.signagetv.dto.PlaylistDto;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.TvService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint usado por la app Android TV para obtener la playlist que debe reproducir AHORA.
 * Requiere JWT del local + header X-Device-Id.
 */
@RestController
@RequestMapping("/api/v1/tv")
@RequiredArgsConstructor
public class TvPlaybackController {

    private final TvService tvService;

    /**
     * Devuelve la playlist que toca reproducir ahora.
     *
     * <p>Cuando no hay nada asignado NO se devuelve un cuerpo vacio ni un 204:
     * se devuelve una playlist valida pero sin items. Las dos alternativas
     * rompian la app ya instalada en los locales:</p>
     * <ul>
     *   <li>200 con cuerpo vacio -> Gson: "End of input at line 1 column 1 path $".</li>
     *   <li>204 -> Retrofit: "response body type was declared as non-null".</li>
     * </ul>
     * <p>En cambio una playlist con {@code items} vacio es un caso que el
     * reproductor ya contempla: muestra el cartel de "no hay contenido" y sigue
     * consultando. Asi el servidor puede avisar "todavia no tenes nada asignado"
     * sin obligar a reinstalar el APK en cada pantalla.</p>
     */
    @GetMapping("/playlist/current")
    public PlaylistDto current(@RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        PlaylistDto pl = tvService.getCurrentPlaylistForDevice(SecurityUtils.currentLocalId(), deviceId);
        return pl != null ? pl : emptyPlaylist();
    }

    /** Playlist "vacia" que el reproductor entiende como "no hay nada que mostrar". */
    private PlaylistDto emptyPlaylist() {
        return PlaylistDto.builder()
                .id(0L)
                .nombre("")
                .transicion("NONE")
                .defaultImageSeconds(8)
                .items(java.util.List.of())
                .audioItems(java.util.List.of())
                .build();
    }
}
