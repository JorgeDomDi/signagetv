-- ============================================================
-- SignageTV - esquema inicial
-- ============================================================

CREATE TABLE locales (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    username        VARCHAR(60)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_locales_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE media_items (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    local_id          BIGINT       NOT NULL,
    filename          VARCHAR(255) NOT NULL,
    storage_path      VARCHAR(500) NOT NULL,
    type              ENUM('IMAGE','VIDEO') NOT NULL,
    mime_type         VARCHAR(80)  NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    duration_seconds  INT          NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_local FOREIGN KEY (local_id) REFERENCES locales(id) ON DELETE CASCADE,
    INDEX idx_media_local (local_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlists (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    local_id               BIGINT       NOT NULL,
    nombre                 VARCHAR(120) NOT NULL,
    transicion             ENUM('FADE','SLIDE','ZOOM','NONE') NOT NULL DEFAULT 'FADE',
    default_image_seconds  INT          NOT NULL DEFAULT 8,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_playlist_local FOREIGN KEY (local_id) REFERENCES locales(id) ON DELETE CASCADE,
    INDEX idx_playlist_local (local_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_items (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id       BIGINT NOT NULL,
    media_item_id     BIGINT NOT NULL,
    position          INT    NOT NULL,
    duration_seconds  INT    NULL,
    CONSTRAINT fk_pli_playlist FOREIGN KEY (playlist_id)   REFERENCES playlists(id)   ON DELETE CASCADE,
    CONSTRAINT fk_pli_media    FOREIGN KEY (media_item_id) REFERENCES media_items(id) ON DELETE CASCADE,
    INDEX idx_pli_playlist (playlist_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tvs (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    local_id             BIGINT       NOT NULL,
    nombre               VARCHAR(120) NOT NULL,
    device_id            VARCHAR(80)  NOT NULL,
    current_playlist_id  BIGINT       NULL,
    last_seen            TIMESTAMP    NULL,
    online               BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tv_local             FOREIGN KEY (local_id)            REFERENCES locales(id)   ON DELETE CASCADE,
    CONSTRAINT fk_tv_current_playlist  FOREIGN KEY (current_playlist_id) REFERENCES playlists(id) ON DELETE SET NULL,
    CONSTRAINT uq_tv_device_local UNIQUE (local_id, device_id),
    INDEX idx_tv_local (local_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE schedules (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    local_id      BIGINT       NOT NULL,
    playlist_id   BIGINT       NOT NULL,
    nombre        VARCHAR(120) NOT NULL,
    dias_semana   VARCHAR(40)  NOT NULL,
    hora_inicio   TIME         NOT NULL,
    hora_fin      TIME         NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    prioridad     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_sched_local    FOREIGN KEY (local_id)    REFERENCES locales(id)   ON DELETE CASCADE,
    CONSTRAINT fk_sched_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    INDEX idx_sched_local (local_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
