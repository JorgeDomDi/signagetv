-- ============================================================
-- SignageTV - V4: repeticiones por item de playlist (videos)
-- ============================================================
--
-- Permite definir cuantas veces se reproduce un video antes de pasar
-- al siguiente item de la playlist. La app de TV NO cambia: el backend
-- expande cada video repetido en N copias consecutivas al servir la
-- playlist al dispositivo, asi que el reproductor simplemente lo repite.
--
-- Para imagenes se ignora (su duracion se controla con duration_seconds).
-- Filas existentes quedan en 1 (comportamiento actual: una sola vez).

ALTER TABLE playlist_items
    ADD COLUMN repeat_count INT NOT NULL DEFAULT 1 AFTER duration_seconds;
