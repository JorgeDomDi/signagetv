package com.signagetv.controller;

import com.signagetv.dto.PlaylistDto;
import com.signagetv.dto.TvAssignPlaylistRequest;
import com.signagetv.dto.TvDto;
import com.signagetv.dto.TvRegisterRequest;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.TvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tvs")
@RequiredArgsConstructor
public class TvController {

    private final TvService tvService;

    @GetMapping
    public List<TvDto> list() {
        return tvService.list(SecurityUtils.currentLocalId());
    }

    @PostMapping("/register")
    public TvDto register(@Valid @RequestBody TvRegisterRequest req) {
        return tvService.register(SecurityUtils.currentLocalId(), req);
    }

    @PutMapping("/{id}/playlist")
    public TvDto assignPlaylist(@PathVariable Long id, @RequestBody TvAssignPlaylistRequest req) {
        return tvService.assignPlaylist(SecurityUtils.currentLocalId(), id, req.getPlaylistId());
    }

    /** Igual que el endpoint de la TV: 204 cuando no hay nada asignado. */
    @GetMapping("/{id}/current")
    public ResponseEntity<PlaylistDto> current(@PathVariable Long id) {
        PlaylistDto pl = tvService.getCurrentPlaylist(SecurityUtils.currentLocalId(), id);
        return pl == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pl);
    }
}
