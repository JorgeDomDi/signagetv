# SignageTV — Arquitectura del Sistema

## 1. Visión general

**SignageTV** es una plataforma de digital signage (cartelería digital) para Android TV con tres componentes:

```
┌─────────────────┐        ┌──────────────────┐        ┌────────────────────┐
│  Panel Admin    │  HTTPS │  Backend API     │   WS   │  App Android TV    │
│  (React)        │◀──────▶│  (Spring Boot)   │◀──────▶│  (Kotlin)          │
│                 │        │  + MySQL         │        │                    │
│ - Login local   │        │  + Storage local │        │ - Login local      │
│ - CRUD playlists│        │                  │        │ - Selector lista   │
│ - Upload media  │        │                  │        │ - Reproductor      │
│ - Horarios      │        │                  │        │ - WebSocket client │
└─────────────────┘        └──────────────────┘        └────────────────────┘
```

Todo se despliega en el **VPS** del usuario vía Docker Compose. Los archivos multimedia (imágenes y videos) se guardan en disco local del VPS.

## 2. Conceptos del dominio

- **Local**: una ubicación física (ej. "Sucursal Centro"). Tiene un usuario y contraseña.
- **TV**: una pantalla Android TV asociada a un local. Un local puede tener N TVs.
- **MediaItem**: un archivo individual (imagen o video) subido por el local.
- **Playlist**: una lista ordenada de MediaItems con duraciones y transiciones (ej. "Menú Desayuno").
- **Schedule**: una regla horaria que asocia una playlist a unos días y un rango horario (ej. "Lunes a Viernes 07:00–11:00").
- **Assignment**: a una TV se le asigna una playlist (manual desde la propia TV en su pantalla de login + selector). Los Schedules permiten que automáticamente cambie según horario si están activos.

## 3. Modelo de datos (MySQL)

```sql
locales (
  id BIGINT PK,
  nombre VARCHAR(120),
  username VARCHAR(60) UNIQUE,
  password_hash VARCHAR(255),
  created_at TIMESTAMP
)

tvs (
  id BIGINT PK,
  local_id BIGINT FK -> locales.id,
  nombre VARCHAR(120),
  device_id VARCHAR(80) UNIQUE,        -- generado en la TV
  current_playlist_id BIGINT FK NULL,  -- lista seleccionada manualmente
  last_seen TIMESTAMP,
  online BOOL
)

media_items (
  id BIGINT PK,
  local_id BIGINT FK -> locales.id,
  filename VARCHAR(255),               -- nombre original
  storage_path VARCHAR(500),           -- ruta en disco del VPS
  type ENUM('IMAGE','VIDEO'),
  mime_type VARCHAR(80),
  size_bytes BIGINT,
  duration_seconds INT NULL,           -- solo videos
  created_at TIMESTAMP
)

playlists (
  id BIGINT PK,
  local_id BIGINT FK -> locales.id,
  nombre VARCHAR(120),
  transicion ENUM('FADE','SLIDE','ZOOM','NONE') DEFAULT 'FADE',
  default_image_seconds INT DEFAULT 8,
  updated_at TIMESTAMP
)

playlist_items (
  id BIGINT PK,
  playlist_id BIGINT FK -> playlists.id,
  media_item_id BIGINT FK -> media_items.id,
  position INT,
  duration_seconds INT NULL            -- override del default
)

schedules (
  id BIGINT PK,
  local_id BIGINT FK -> locales.id,
  playlist_id BIGINT FK -> playlists.id,
  nombre VARCHAR(120),                 -- ej. "Desayuno"
  dias_semana VARCHAR(20),             -- bitmask CSV: "MON,TUE,WED,THU,FRI"
  hora_inicio TIME,                    -- 07:00
  hora_fin TIME,                       -- 11:00
  activo BOOL DEFAULT TRUE,
  prioridad INT DEFAULT 0
)
```

## 4. API REST (endpoints principales)

Base path: `/api/v1`

### Autenticación
- `POST /auth/login` — body `{username, password}` → `{token, local}`
- `GET  /auth/me` — devuelve el local actual (necesita JWT)

### Media (subida de archivos)
- `POST   /media/upload` — multipart/form-data, file
- `GET    /media` — lista del local actual
- `DELETE /media/{id}`
- `GET    /media/{id}/file` — descarga binaria (usado por la TV)

### Playlists
- `GET    /playlists` — lista del local
- `POST   /playlists` — `{nombre, transicion, default_image_seconds}`
- `PUT    /playlists/{id}` — actualizar configuración general
- `DELETE /playlists/{id}`
- `PUT    /playlists/{id}/items` — reemplazar items: `[{media_item_id, position, duration_seconds}]`

### Schedules
- `GET    /schedules` — lista
- `POST   /schedules` — crear
- `PUT    /schedules/{id}` — actualizar
- `DELETE /schedules/{id}`

### TVs
- `GET    /tvs` — TVs del local
- `POST   /tvs/register` — registro desde la app Android: `{device_id, nombre}`
- `PUT    /tvs/{id}/playlist` — asignar playlist manual
- `GET    /tvs/{id}/current` — devuelve playlist activa AHORA (resuelve schedules + selección manual)

### Endpoint clave para la TV
- `GET /tv/playlist/current` — la app TV (autenticada) consulta qué reproducir. La lógica:
  1. ¿Hay un Schedule activo ahora? → devuelve esa playlist.
  2. Si no → devuelve la `current_playlist_id` seleccionada manualmente.
  3. Si tampoco → devuelve null/empty.

## 5. WebSocket (push en tiempo real)

Endpoint: `/ws` (STOMP sobre SockJS).

**Canales:**
- `/topic/local/{localId}/playlists` — cualquier cambio en playlists/items/schedules de ese local.
- `/topic/tv/{tvId}/command` — comandos directos a una TV (forzar refresh, mostrar mensaje).

**Flujo:**
1. Admin guarda cambios → backend persiste.
2. Backend publica evento en `/topic/local/{localId}/playlists`.
3. Todas las TVs de ese local reciben el evento → llaman a `GET /tv/playlist/current` para resincronizar.

**Fallback:** la app Android también hace polling cada 60 segundos por si el WebSocket se cae.

## 6. Storage de archivos en VPS

```
/var/signagetv/media/
  └── locales/
       ├── 1/
       │    ├── 2026-05-27_abc123.jpg
       │    └── 2026-05-27_def456.mp4
       └── 2/
            └── ...
```

El backend genera nombres únicos (`UUID + timestamp + extensión original`) para evitar colisiones. La ruta `storage_path` se guarda en BD.

Descarga: el endpoint `/media/{id}/file` valida que el `media_item` pertenezca al local autenticado antes de servir el archivo.

## 7. Flujo de uso (usuario final)

### Admin (panel web)
1. El dueño del local entra a `https://signage.tu-dominio.com` con su usuario/contraseña.
2. Sube imágenes/videos a su biblioteca.
3. Crea una playlist "Menú Desayuno", arrastra items, configura duración por imagen y transición.
4. Crea un Schedule: "Lunes a Viernes, 07:00–11:00, playlist Desayuno".
5. Guarda → las TVs del local reciben el cambio por WebSocket y se actualizan al instante.

### TV (Android TV)
1. Se instala el APK en la TV (sideload o Play Store interna).
2. Al abrir, pide usuario/contraseña del local.
3. Muestra la lista de playlists disponibles → el usuario elige una manualmente (o "Automático según horario").
4. Empieza a reproducir. Si hay un schedule activo, lo respeta; si no, reproduce la elegida.
5. Queda escuchando el WebSocket: cualquier cambio en el panel se refleja al instante.

## 8. Stack técnico

| Componente   | Tecnología |
|--------------|------------|
| Backend      | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Spring WebSocket (STOMP) |
| Base de datos| MySQL 8 |
| Storage      | Disco local del VPS |
| Frontend     | React 18 + Vite, React Router, Axios, TailwindCSS, react-beautiful-dnd (drag&drop), SockJS+Stomp client |
| App TV       | Android (Kotlin), Min SDK 21 (Android TV 5.0), ExoPlayer (Media3), Retrofit, OkHttp WebSocket, Coil (imágenes) |
| Despliegue   | Docker + Docker Compose, Nginx como reverse proxy |

## 9. Seguridad

- Contraseñas con BCrypt.
- JWT firmado HS256, expiración 24h, refresh implícito al loguear.
- Cada request valida que el recurso pertenezca al `local_id` del token (aislamiento multi-tenant).
- HTTPS terminado en Nginx (Let's Encrypt vía certbot).
- CORS configurado para el dominio del admin panel.

## 10. Próximos pasos (post-MVP)

- Reportes de reproducción (qué se mostró y cuándo).
- Soporte multi-pantalla (zonas dentro de la misma TV).
- Editor visual de transiciones.
- Notificaciones push a TVs específicas (mostrar mensaje urgente).
- Plantillas con texto sobre imagen.
