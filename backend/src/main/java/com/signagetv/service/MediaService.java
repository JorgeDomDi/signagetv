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
    private final UrlBuilder urlBuilder;

    @Transactional
    public MediaItemDto upload(Long localId, MultipartFile file) {
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        MediaType type = detectType(mime, file.getOriginalFilename());

        StorageService.StoredFile stored = storageService.store(localId, file);

        MediaItem item = MediaItem.builder()
                .localId(localId)
                .filename(stored.originalName())
                .storagePath(stored.absolutePath())
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
        mediaRepo.delete(item);
    }

    public MediaItem getOwned(Long localId, Long id) {
        return mediaRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Media no encontrado"));
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
                .createdAt(m.getCreatedAt())
                .build();
    }

    private MediaType detectType(String mime, String filename) {
        if (mime != null) {
            if (mime.startsWith("image/")) return MediaType.IMAGE;
            if (mime.startsWith("video/")) return MediaType.VIDEO;
        }
        if (filename != null) {
            String f = filename.toLowerCase();
            if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") ||
                f.endsWith(".gif") || f.endsWith(".webp")) return MediaType.IMAGE;
            if (f.endsWith(".mp4") || f.endsWith(".mov") || f.endsWith(".mkv") ||
                f.endsWith(".webm") || f.endsWith(".avi")) return MediaType.VIDEO;
        }
        throw new BadRequestException("Tipo de archivo no soportado: " + mime);
    }
}
