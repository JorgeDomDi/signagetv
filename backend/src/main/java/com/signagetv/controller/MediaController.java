package com.signagetv.controller;

import com.signagetv.dto.MediaItemDto;
import com.signagetv.entity.MediaItem;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.MediaService;
import com.signagetv.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final StorageService storageService;

    /**
     * Sube un archivo. El panel manda tambien {@code duration_seconds} cuando puede
     * leerlo del propio archivo en el navegador (audio y video), asi no hace falta
     * decodificar nada en el servidor.
     */
    @PostMapping("/upload")
    public MediaItemDto upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "duration_seconds", required = false)
                               Integer durationSeconds) {
        return mediaService.upload(SecurityUtils.currentLocalId(), file, durationSeconds);
    }

    /**
     * Rellena la duracion de un archivo subido antes de que existiera este dato.
     * El panel la calcula al vuelo y la manda una sola vez.
     */
    @PutMapping("/{id}/duration")
    public MediaItemDto setDuration(@PathVariable Long id, @RequestBody DurationRequest req) {
        return mediaService.setDurationIfMissing(SecurityUtils.currentLocalId(), id, req.getSeconds());
    }

    @lombok.Data
    public static class DurationRequest {
        private Integer seconds;
    }

    @GetMapping
    public List<MediaItemDto> list() {
        return mediaService.list(SecurityUtils.currentLocalId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mediaService.delete(SecurityUtils.currentLocalId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Trozo maximo que se devuelve por peticion parcial. */
    private static final long RANGE_CHUNK_BYTES = 2L * 1024 * 1024;

    /**
     * Descarga del archivo original.
     *
     * <p>Soporta peticiones parciales (cabecera {@code Range}). Sin esto, escuchar
     * un MP3 de 200 MB desde el panel obligaba a bajarlo entero antes de oir nada
     * y no se podia adelantar. La app de TV no manda Range: recibe el archivo
     * completo como siempre.</p>
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Object> file(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader)
            throws Exception {

        MediaItem item = mediaService.getOwned(SecurityUtils.currentLocalId(), id);
        Path path = storageService.resolve(item.getStoragePath());

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mt;
        try {
            mt = MediaType.parseMediaType(item.getMimeType());
        } catch (Exception e) {
            mt = MediaType.APPLICATION_OCTET_STREAM;
        }

        long size = Files.size(path);
        FileSystemResource resource = new FileSystemResource(path);

        if (rangeHeader != null && !rangeHeader.isBlank()) {
            try {
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                if (!ranges.isEmpty()) {
                    HttpRange r = ranges.get(0);
                    long start = r.getRangeStart(size);
                    if (start >= size) {
                        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + size)
                                .build();
                    }
                    long end = r.getRangeEnd(size);
                    long count = Math.min(RANGE_CHUNK_BYTES, end - start + 1);
                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .contentType(mt)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                            .body(new ResourceRegion(resource, start, count));
                }
            } catch (IllegalArgumentException ex) {
                // Cabecera Range malformada: devolvemos el archivo entero.
            }
        }

        return ResponseEntity.ok()
                .contentType(mt)
                .contentLength(size)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + item.getFilename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }

    /**
     * Devuelve la miniatura de baja resolución de una imagen, para el panel admin.
     * Si no se puede generar (formato no legible, etc.) degrada al archivo original.
     * La generación es lazy: las imágenes subidas antes de esta funcionalidad se
     * procesan la primera vez que se piden.
     */
    @GetMapping("/{id}/thumb")
    public ResponseEntity<Object> thumb(@PathVariable Long id) throws Exception {
        Long localId = SecurityUtils.currentLocalId();
        String thumbPath = mediaService.resolveThumbnailPath(localId, id);

        // Fallback al original si no hay miniatura (p. ej. formato no soportado).
        if (thumbPath == null) {
            return file(id, null);
        }

        Path path = storageService.resolve(thumbPath);
        if (!Files.exists(path)) {
            return file(id, null);
        }

        long size = Files.size(path);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(size)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new FileSystemResource(path));
    }
}
