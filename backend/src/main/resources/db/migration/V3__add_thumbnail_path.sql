-- ============================================================
-- SignageTV - V3: ruta de miniatura en `media_items`
-- ============================================================
--
-- Guarda la ruta en disco de una miniatura de baja resolución
-- generada al subir imágenes. El panel admin (biblioteca y editor
-- de playlists) usa la miniatura en lugar del archivo original para
-- cargar mucho más rápido. La app de TV sigue usando el original.
--
-- NULL = todavía no se generó (se genera de forma lazy la primera
-- vez que se pide /media/{id}/thumb). Las imágenes ya subidas antes
-- de esta migración quedan en NULL y se rellenan al vuelo.

ALTER TABLE media_items
    ADD COLUMN thumbnail_path VARCHAR(500) NULL AFTER storage_path;
