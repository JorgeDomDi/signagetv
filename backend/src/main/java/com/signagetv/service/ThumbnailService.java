package com.signagetv.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Genera miniaturas de baja resolución para las imágenes subidas.
 *
 * Objetivo: que el panel admin (biblioteca y editor de playlists) cargue
 * thumbnails de ~40-80 KB en lugar de los originales de varios MB. La app
 * de TV sigue usando los archivos originales (endpoint /media/{id}/file).
 *
 * Las miniaturas se guardan junto al original, en una subcarpeta `thumbs/`,
 * siempre como JPEG. Soporta JPG, PNG, GIF y WebP (este último vía el plugin
 * TwelveMonkeys, registrado automáticamente por ImageIO).
 */
@Slf4j
@Service
public class ThumbnailService {

    /** Ancho/alto máximo de la miniatura en píxeles. */
    private static final int MAX_DIMENSION = 480;
    /** Calidad JPEG de salida (0..1). */
    private static final double JPEG_QUALITY = 0.72;

    /**
     * Genera la miniatura a partir de la ruta del archivo original de imagen.
     *
     * @param originalPath ruta absoluta del archivo original
     * @return ruta absoluta de la miniatura generada, o {@code null} si falló
     *         (en cuyo caso el caller debe degradar al archivo original)
     */
    public String generate(String originalPath) {
        try {
            Path original = Paths.get(originalPath);
            if (!Files.exists(original)) {
                log.warn("No existe el original para miniatura: {}", originalPath);
                return null;
            }

            Path thumbsDir = original.getParent().resolve("thumbs");
            Files.createDirectories(thumbsDir);

            String base = stripExtension(original.getFileName().toString());
            Path thumb = thumbsDir.resolve(base + ".jpg");

            Thumbnails.of(original.toFile())
                    .size(MAX_DIMENSION, MAX_DIMENSION)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toFile(thumb.toFile());

            log.info("Miniatura generada: {} ({} bytes)", thumb, Files.size(thumb));
            return thumb.toAbsolutePath().toString();
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo generar miniatura de {}: {}", originalPath, e.getMessage());
            return null;
        }
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}
