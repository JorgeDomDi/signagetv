package com.signagetv.service;

import com.signagetv.dto.MediaItemDto;
import com.signagetv.entity.MediaItem;
import com.signagetv.entity.MediaType;
import com.signagetv.exception.BadRequestException;
import com.signagetv.exception.NotFoundException;
import com.signagetv.repository.MediaItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaItemRepository mediaRepo;
    private final StorageService storageService;
    private final ThumbnailService thumbnailService;
    private final UrlBuilder urlBuilder;
    private final RealtimeNotificationService realtime;

    @Transactional
    public MediaItemDto upload(Long localId, MultipartFile file) {
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        MediaType type = detectType(mime, file.getOriginalFilename());

        StorageService.StoredFile stored = storageService.store(localId, file);

        // Para imágenes, generamos una miniatura de baja resolución para el panel admin.
        String thumbPath = null;
        if (type == MediaType.IMAGE) {
            thumbPath = thumbnailService.generate(stored.absolutePath());
        }

        MediaItem item = MediaItem.builder()
                .localId(localId)
                .filename(stored.originalName())
                .storagePath(stored.absolutePath())
                .thumbnailPath(thumbPath)
                .type(type)
                .mimeType(mime)
                .sizeBytes(file.getSize())
                .build();

        item = mediaRepo.save(item);
        log.info("Media subido id={} local={} type={} bytes={}", item.getId(), localId, type, item.getSizeBytes());
        return toDto(item);
    }

    public List<MediaItemDto> list(Long localId) {
        return mediaRepo.findByLocalIdOrderByCreatedAtDesc(localId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long localId, Long id) {
        MediaItem item = mediaRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Media no encontrado"));
        storageService.delete(item.getStoragePath());
        if (item.getThumbnailPath() != null) {
            storageService.delete(item.getThumbnailPath());
        }
        mediaRepo.delete(item);

        // Si el archivo estaba en alguna playlist, las TVs tienen que enterarse ya:
        // el borrado en cascada lo saca de la lista y si no avisamos seguirian
        // pidiendo una URL que ya no existe hasta el proximo poll.
        realtime.notifyPlaylistsChanged(localId);
    }

    public MediaItem getOwned(Long localId, Long id) {
        return mediaRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Media no encontrado"));
    }

    /**
     * Devuelve la ruta de la miniatura de una imagen, generándola al vuelo si todavía
     * no existe (caso de imágenes subidas antes de esta funcionalidad). Si no se puede
     * generar (p. ej. es un video o el formato no es legible), devuelve {@code null}
     * para que el caller degrade al archivo original.
     */
    @Transactional
    public String resolveThumbnailPath(Long localId, Long id) {
        MediaItem item = getOwned(localId, id);
        if (item.getType() != MediaType.IMAGE) return null;

        String thumb = item.getThumbnailPath();
        if (thumb != null && java.nio.file.Files.exists(java.nio.file.Paths.get(thumb))) {
            return thumb;
        }

        // Generación lazy + persistencia de la ruta.
        String generated = thumbnailService.generate(item.getStoragePath());
        if (generated != null) {
            item.setThumbnailPath(generated);
            mediaRepo.save(item);
        }
        return generated;
    }

    public MediaItemDto toDto(MediaItem m) {
        return MediaItemDto.builder()
                .id(m.getId())
                .filename(m.getFilename())
                .type(m.getType().name())
                .mimeType(m.getMimeType())
                .sizeBytes(m.getSizeBytes())
                .durationSeconds(m.getDurationSeconds())
                .url(urlBuilder.mediaFileUrl(m.getId()))
                .thumbnailUrl(m.getType() == MediaType.IMAGE ? urlBuilder.mediaThumbUrl(m.getId()) : null)
                .createdAt(m.getCreatedAt())
                .build();
    }

    private MediaType detectType(String mime, String filename) {
        if (mime != null) {
            if (mime.startsWith("image/")) return MediaType.IMAGE;
            if (mime.startsWith("video/")) return MediaType.VIDEO;
            if (mime.startsWith("audio/")) return MediaType.AUDIO;
        }
        if (filename != null) {
            String f = filename.toLowerCase();
            if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") ||
                f.endsWith(".gif") || f.endsWith(".webp")) return MediaType.IMAGE;
            if (f.endsWith(".mp4") || f.endsWith(".mov") || f.endsWith(".mkv") ||
                f.endsWith(".webm") || f.endsWith(".avi")) return MediaType.VIDEO;
            if (f.endsWith(".mp3") || f.endsWith(".m4a") || f.endsWith(".aac") ||
                f.endsWith(".wav") || f.endsWith(".ogg") || f.endsWith(".oga") ||
                f.endsWith(".opus") || f.endsWith(".flac")) return MediaType.AUDIO;
        }
        throw new BadRequestException("Tipo de archivo no soportado: " + mime);
    }
}
