package com.signagetv.controller;

import com.signagetv.dto.PlaylistCreateRequest;
import com.signagetv.dto.PlaylistDto;
import com.signagetv.dto.PlaylistItemsReplaceRequest;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    public List<PlaylistDto> list() {
        return playlistService.list(SecurityUtils.currentLocalId());
    }

    @GetMapping("/{id}")
    public PlaylistDto get(@PathVariable Long id) {
        return playlistService.get(SecurityUtils.currentLocalId(), id);
    }

    @PostMapping
    public PlaylistDto create(@Valid @RequestBody PlaylistCreateRequest req) {
        return playlistService.create(SecurityUtils.currentLocalId(), req);
    }

    @PutMapping("/{id}")
    public PlaylistDto update(@PathVariable Long id, @RequestBody PlaylistCreateRequest req) {
        return playlistService.update(SecurityUtils.currentLocalId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        playlistService.delete(SecurityUtils.currentLocalId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/items")
    public PlaylistDto replaceItems(@PathVariable Long id, @RequestBody PlaylistItemsReplaceRequest req) {
        return playlistService.replaceItems(SecurityUtils.currentLocalId(), id, req);
    }
}
