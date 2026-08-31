-- ============================================================
-- SignageTV - V5: pistas de audio (musica de fondo) por playlist
-- ============================================================
--
-- 1) `media_items.type` admite ahora AUDIO ademas de IMAGE y VIDEO.
--    Las filas existentes no cambian (siguen siendo IMAGE o VIDEO).
--
-- 2) Nueva tabla `playlist_audio_items`: la lista de pistas de musica
--    de fondo de una playlist, ordenada por `position`.
--
--    Va en una tabla aparte de `playlist_items` a proposito: el audio NO
--    forma parte de la rotacion visual. Las imagenes y videos siguen su
--    ciclo por un lado y la musica corre por el suyo, en loop infinito
--    sobre toda la lista (al terminar la ultima pista vuelve a la primera).
--
--    Playlists existentes quedan sin pistas -> se comportan igual que antes.

ALTER TABLE media_items
    MODIFY COLUMN type ENUM('IMAGE','VIDEO','AUDIO') NOT NULL;

CREATE TABLE playlist_audio_items (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id    BIGINT NOT NULL,
    media_item_id  BIGINT NOT NULL,
    position       INT    NOT NULL,
    CONSTRAINT fk_pai_playlist FOREIGN KEY (playlist_id)   REFERENCES playlists(id)   ON DELETE CASCADE,
    CONSTRAINT fk_pai_media    FOREIGN KEY (media_item_id) REFERENCES media_items(id) ON DELETE CASCADE,
    INDEX idx_pai_playlist (playlist_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
