package com.signagetv.controller;

import com.signagetv.dto.PlaylistDto;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.TvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
     * Devuelve la playlist que toca reproducir ahora, o <b>204 No Content</b> si no
     * hay nada asignado.
     *
     * <p>Antes devolvia 200 con el cuerpo vacio y el cliente reventaba al parsear
     * ("End of input at line 1 column 1 path $"), mostrando ese error en pantalla
     * en vez del cartel de "sin contenido". Con 204 el cliente recibe null limpio.</p>
     */
    @GetMapping("/playlist/current")
    public ResponseEntity<PlaylistDto> current(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        PlaylistDto pl = tvService.getCurrentPlaylistForDevice(SecurityUtils.currentLocalId(), deviceId);
        return pl == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pl);
    }
}
