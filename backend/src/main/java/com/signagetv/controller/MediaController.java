package com.signagetv.controller;

import com.signagetv.dto.MediaItemDto;
import com.signagetv.entity.MediaItem;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.MediaService;
import com.signagetv.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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

    @PostMapping("/upload")
    public MediaItemDto upload(@RequestParam("file") MultipartFile file) {
        return mediaService.upload(SecurityUtils.currentLocalId(), file);
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

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) throws Exception {
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
        return ResponseEntity.ok()
                .contentType(mt)
                .contentLength(size)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + item.getFilename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new FileSystemResource(path));
    }
}
