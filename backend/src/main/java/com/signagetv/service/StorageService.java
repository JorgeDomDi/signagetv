package com.signagetv.service;

import com.signagetv.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Gestiona la persistencia de archivos en el disco local del VPS.
 * Estructura: {basePath}/locales/{localId}/{YYYY-MM-DD_uuid.ext}
 */
@Slf4j
@Service
public class StorageService {

    @Value("${app.storage.path}")
    private String basePath;

    @PostConstruct
    void init() throws IOException {
        Path root = Paths.get(basePath);
        Files.createDirectories(root);
        log.info("Storage inicializado en {}", root.toAbsolutePath());
    }

    /**
     * Guarda el archivo del Local en disco y devuelve la ruta absoluta donde quedó.
     */
    public StoredFile store(Long localId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Archivo vacío");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) original = "archivo";
        String ext = extractExtension(original);

        String fname = LocalDate.now() + "_" + UUID.randomUUID() + ext;
        Path localDir = Paths.get(basePath, "locales", String.valueOf(localId));
        try {
            Files.createDirectories(localDir);
            Path target = localDir.resolve(fname);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(target.toAbsolutePath().toString(), fname, original);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo almacenar el archivo: " + e.getMessage(), e);
        }
    }

    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            log.warn("No se pudo eliminar {}: {}", storagePath, e.getMessage());
        }
    }

    public Path resolve(String storagePath) {
        return Paths.get(storagePath);
    }

    private String extractExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : "";
    }

    public record StoredFile(String absolutePath, String storedName, String originalName) {}
}
