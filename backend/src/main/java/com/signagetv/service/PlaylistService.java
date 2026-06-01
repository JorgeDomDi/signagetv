package com.signagetv.service;

import com.signagetv.dto.*;
import com.signagetv.entity.MediaItem;
import com.signagetv.entity.Playlist;
import com.signagetv.entity.PlaylistItem;
import com.signagetv.entity.Transicion;
import com.signagetv.exception.BadRequestException;
import com.signagetv.exception.NotFoundException;
import com.signagetv.repository.MediaItemRepository;
import com.signagetv.repository.PlaylistItemRepository;
import com.signagetv.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepo;
    private final PlaylistItemRepository itemRepo;
    private final MediaItemRepository mediaRepo;
    private final MediaService mediaService;
    private final RealtimeNotificationService realtime;

    public List<PlaylistDto> list(Long localId) {
        return playlistRepo.findByLocalIdOrderByNombreAsc(localId).stream()
                .map(this::buildPlaylistDto)
                .toList();
    }

    public PlaylistDto get(Long localId, Long id) {
        Playlist p = playlistRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Playlist no encontrada"));
        List<PlaylistItem> items = itemRepo.findByPlaylistIdOrderByPositionAsc(p.getId());
        return toDto(p, buildItemDtos(items, localId));
    }

    @Transactional
    public PlaylistDto create(Long localId, PlaylistCreateRequest req) {
        Playlist p = Playlist.builder()
                .localId(localId)
                .nombre(req.getNombre())
                .transicion(parseTransicion(req.getTransicion()))
                .defaultImageSeconds(req.getDefaultImageSeconds() != null ? req.getDefaultImageSeconds() : 8)
                .build();
        p = playlistRepo.save(p);
        realtime.notifyPlaylistsChanged(localId);
        return toDto(p, List.of());
    }

    @Transactional
    public PlaylistDto update(Long localId, Long id, PlaylistCreateRequest req) {
        Playlist p = playlistRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Playlist no encontrada"));
        if (req.getNombre() != null) p.setNombre(req.getNombre());
        if (req.getTransicion() != null) p.setTransicion(parseTransicion(req.getTransicion()));
        if (req.getDefaultImageSeconds() != null) p.setDefaultImageSeconds(req.getDefaultImageSeconds());
        p = playlistRepo.save(p);
        realtime.notifyPlaylistsChanged(localId);

        List<PlaylistItem> items = itemRepo.findByPlaylistIdOrderByPositionAsc(p.getId());
        return toDto(p, buildItemDtos(items, localId));
    }

    @Transactional
    public void delete(Long localId, Long id) {
        Playlist p = playlistRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Playlist no encontrada"));
        playlistRepo.delete(p);
        realtime.notifyPlaylistsChanged(localId);
    }

    @Transactional
    public PlaylistDto replaceItems(Long localId, Long playlistId, PlaylistItemsReplaceRequest req) {
        Playlist p = playlistRepo.findByIdAndLocalId(playlistId, localId)
                .orElseThrow(() -> new NotFoundException("Playlist no encontrada"));

        // Borrar items previos
        itemRepo.deleteByPlaylistId(p.getId());

        if (req.getItems() != null) {
            // Validar que todos los mediaItemId pertenezcan al local
            for (PlaylistItemsReplaceRequest.Item it : req.getItems()) {
                if (it.getMediaItemId() == null) throw new BadRequestException("mediaItemId requerido");
                mediaRepo.findByIdAndLocalId(it.getMediaItemId(), localId)
                        .orElseThrow(() -> new BadRequestException(
                                "MediaItem " + it.getMediaItemId() + " no pertenece al local"));
            }

            int idx = 0;
            for (PlaylistItemsReplaceRequest.Item it : req.getItems()) {
                Integer rc = it.getRepeatCount();
                int repeat = (rc == null || rc < 1) ? 1 : Math.min(rc, 100);
                PlaylistItem pli = PlaylistItem.builder()
                        .playlistId(p.getId())
                        .mediaItemId(it.getMediaItemId())
                        .position(it.getPosition() != null ? it.getPosition() : idx)
                        .durationSeconds(it.getDurationSeconds())
                        .repeatCount(repeat)
                        .build();
                itemRepo.save(pli);
                idx++;
            }
        }

        // Touch updated_at explícito
        p.setUpdatedAt(java.time.LocalDateTime.now());
        playlistRepo.save(p);
        realtime.notifyPlaylistsChanged(localId);

        List<PlaylistItem> items = itemRepo.findByPlaylistIdOrderByPositionAsc(p.getId());
        return toDto(p, buildItemDtos(items, localId));
    }

    /**
     * Versión interna para uso desde TvService — devuelve la playlist con items.
     * Expande los videos con repeat_count > 1 en N copias consecutivas, de modo que
     * la app de TV (que no conoce repeat_count) los reproduzca repetidos sin cambios.
     */
    public PlaylistDto buildPlaylistDto(Playlist p) {
        List<PlaylistItem> items = itemRepo.findByPlaylistIdOrderByPositionAsc(p.getId());
        List<PlaylistItemDto> dtos = buildItemDtos(items, p.getLocalId());
        List<PlaylistItemDto> expanded = new java.util.ArrayList<>();
        int pos = 0;
        for (PlaylistItemDto d : dtos) {
            int times = 1;
            if (d.getMedia() != null
                    && "VIDEO".equalsIgnoreCase(d.getMedia().getType())
                    && d.getRepeatCount() != null && d.getRepeatCount() > 1) {
                times = d.getRepeatCount();
            }
            for (int i = 0; i < times; i++) {
                expanded.add(PlaylistItemDto.builder()
                        .id(d.getId())
                        .position(pos++)
                        .durationSeconds(d.getDurationSeconds())
                        .repeatCount(1)
                        .media(d.getMedia())
                        .build());
            }
        }
        return toDto(p, expanded);
    }

    private List<PlaylistItemDto> buildItemDtos(List<PlaylistItem> items, Long localId) {
        if (items.isEmpty()) return List.of();
        List<Long> mediaIds = items.stream().map(PlaylistItem::getMediaItemId).toList();
        Map<Long, MediaItem> mediaById = mediaRepo.findAllById(mediaIds).stream()
                .filter(m -> m.getLocalId().equals(localId))
                .collect(Collectors.toMap(MediaItem::getId, m -> m));

        return items.stream()
                .filter(pi -> mediaById.containsKey(pi.getMediaItemId()))
                .map(pi -> PlaylistItemDto.builder()
                        .id(pi.getId())
                        .position(pi.getPosition())
                        .durationSeconds(pi.getDurationSeconds())
                        .repeatCount(pi.getRepeatCount() != null ? pi.getRepeatCount() : 1)
                        .media(mediaService.toDto(mediaById.get(pi.getMediaItemId())))
                        .build())
                .toList();
    }

    private PlaylistDto toDto(Playlist p, List<PlaylistItemDto> items) {
        return PlaylistDto.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .transicion(p.getTransicion().name())
                .defaultImageSeconds(p.getDefaultImageSeconds())
                .updatedAt(p.getUpdatedAt())
                .items(items)
                .build();
    }

    private Transicion parseTransicion(String s) {
        if (s == null || s.isBlank()) return Transicion.FADE;
        try {
            return Transicion.valueOf(s.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Transición inválida: " + s);
        }
    }
}
