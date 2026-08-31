package com.signagetv.service;

import com.signagetv.dto.*;
import com.signagetv.entity.MediaItem;
import com.signagetv.entity.MediaType;
import com.signagetv.entity.Playlist;
import com.signagetv.entity.PlaylistAudioItem;
import com.signagetv.entity.PlaylistItem;
import com.signagetv.entity.Transicion;
import com.signagetv.exception.BadRequestException;
import com.signagetv.exception.NotFoundException;
import com.signagetv.repository.MediaItemRepository;
import com.signagetv.repository.PlaylistAudioItemRepository;
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
    private final PlaylistAudioItemRepository audioRepo;
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
                MediaItem m = mediaRepo.findByIdAndLocalId(it.getMediaItemId(), localId)
                        .orElseThrow(() -> new BadRequestException(
                                "MediaItem " + it.getMediaItemId() + " no pertenece al local"));
                // El audio va en su propia lista (musica de fondo), nunca en la rotacion visual.
                if (m.getType() == MediaType.AUDIO) {
                    throw new BadRequestException(
                            "'" + m.getFilename() + "' es una pista de audio: agregala en Musica de fondo, "
                            + "no en el contenido de la playlist");
                }
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
     * Cada item incluye repeat_count; la app de TV reproduce el video en loop nativo
     * esa cantidad de veces (sin cortes) antes de avanzar al siguiente item.
     */
    /**
     * Reemplaza por completo la lista de pistas de musica de fondo de una playlist.
     * Una lista vacia (o null) deja la playlist sin musica.
     */
    @Transactional
    public PlaylistDto replaceAudioItems(Long localId, Long playlistId, PlaylistAudioItemsReplaceRequest req) {
        Playlist p = playlistRepo.findByIdAndLocalId(playlistId, localId)
                .orElseThrow(() -> new NotFoundException("Playlist no encontrada"));

        audioRepo.deleteByPlaylistId(p.getId());

        if (req != null && req.getItems() != null) {
            int idx = 0;
            for (PlaylistAudioItemsReplaceRequest.Item it : req.getItems()) {
                if (it.getMediaItemId() == null) throw new BadRequestException("mediaItemId requerido");
                MediaItem m = mediaRepo.findByIdAndLocalId(it.getMediaItemId(), localId)
                        .orElseThrow(() -> new BadRequestException(
                                "MediaItem " + it.getMediaItemId() + " no pertenece al local"));
                if (m.getType() != MediaType.AUDIO) {
                    throw new BadRequestException("'" + m.getFilename() + "' no es un archivo de audio");
                }
                audioRepo.save(PlaylistAudioItem.builder()
                        .playlistId(p.getId())
                        .mediaItemId(m.getId())
                        .position(it.getPosition() != null ? it.getPosition() : idx)
                        .build());
                idx++;
            }
        }

        // Touch updated_at para que las TVs detecten el cambio y recarguen.
        p.setUpdatedAt(java.time.LocalDateTime.now());
        playlistRepo.save(p);
        realtime.notifyPlaylistsChanged(localId);

        List<PlaylistItem> items = itemRepo.findByPlaylistIdOrderByPositionAsc(p.getId());
        return toDto(p, buildItemDtos(items, localId));
    }

    public PlaylistDto buildPlaylistDto(Playlist p) {
        List<PlaylistItem> items = itemRepo.findByPlaylistIdOrderByPositionAsc(p.getId());
        return toDto(p, buildItemDtos(items, p.getLocalId()));
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

    /** Pistas de musica de fondo de la playlist, en orden, con su media expandido. */
    private List<PlaylistAudioItemDto> buildAudioItemDtos(Long playlistId, Long localId) {
        List<PlaylistAudioItem> tracks = audioRepo.findByPlaylistIdOrderByPositionAsc(playlistId);
        if (tracks.isEmpty()) return List.of();

        List<Long> mediaIds = tracks.stream().map(PlaylistAudioItem::getMediaItemId).toList();
        Map<Long, MediaItem> mediaById = mediaRepo.findAllById(mediaIds).stream()
                .filter(m -> m.getLocalId().equals(localId))
                .collect(Collectors.toMap(MediaItem::getId, m -> m));

        return tracks.stream()
                .filter(t -> mediaById.containsKey(t.getMediaItemId()))
                .map(t -> PlaylistAudioItemDto.builder()
                        .id(t.getId())
                        .position(t.getPosition())
                        .media(mediaService.toDto(mediaById.get(t.getMediaItemId())))
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
                .audioItems(buildAudioItemDtos(p.getId(), p.getLocalId()))
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
