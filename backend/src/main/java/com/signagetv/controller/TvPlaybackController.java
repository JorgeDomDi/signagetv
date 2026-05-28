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

    @GetMapping("/playlist/current")
    public PlaylistDto current(@RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return tvService.getCurrentPlaylistForDevice(SecurityUtils.currentLocalId(), deviceId);
    }
}
