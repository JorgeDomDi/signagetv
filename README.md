# SignageTV

Sistema de cartelería digital (digital signage) para Android TV. Cada **local** entra desde un panel web a su cuenta, sube imágenes y videos, organiza **playlists**, define **horarios** (ej. menú de desayuno L-V 7-11h) y los reproduce en sus **TVs Android**. Los cambios se propagan en tiempo real por WebSocket.

```
┌─────────────────┐        ┌──────────────────┐        ┌────────────────────┐
│  Panel Admin    │  HTTPS │  Backend API     │   WS   │  App Android TV    │
│  React + Vite   │◀──────▶│  Spring Boot 3   │◀──────▶│  Kotlin + ExoPlayer│
│                 │        │  + MySQL 8       │        │                    │
└─────────────────┘        └──────────────────┘        └────────────────────┘
                    Todo se despliega en tu VPS con docker-compose
```

## Estructura del repositorio

```
androidTV/
├── backend/          Spring Boot 3 + MySQL + JWT + WebSocket
├── admin-panel/      React 18 + TypeScript + Tailwind + Vite
├── android-tv-app/   App Android TV nativa (Kotlin + Media3)
├── deploy/           Nginx reverse proxy
├── docs/             Documentación técnica
├── docker-compose.yml
└── .env.example      Variables de entorno (copiar a .env)
```

Documentación detallada de arquitectura, modelo de datos y endpoints: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Desarrollo local

### Backend (puerto 8080)

```bash
cd backend
# requiere Java 17 + Maven + MySQL 8 corriendo en localhost:3306
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

El perfil `dev` crea dos locales de prueba:

- `local1` / `password123`
- `local2` / `password123`

### Panel admin (puerto 5173)

```bash
cd admin-panel
cp .env.example .env   # asegurarse de que VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

Abre <http://localhost:5173> y entra con `local1` / `password123`.

### App Android TV

1. Abre `android-tv-app/` en Android Studio (Hedgehog 2023.1.1 o superior).
2. Sincroniza Gradle (descargará SDK 34 si no está).
3. Conecta tu TV Android vía adb: `adb connect <ip-tv>:5555`.
4. Run > app, o `Build > Generate Signed APK` para un APK distribuible.
5. En la pantalla de login de la app, escribe:
   - **URL del servidor**: `http://<ip-de-tu-pc>:8080`
   - **Usuario** / **Contraseña**: los del local

---

## Despliegue en VPS (producción)

Requisitos: VPS con Docker y Docker Compose. Apunta tu dominio (ej. `signage.tu-dominio.com`) al VPS.

```bash
# 1. Clonar el proyecto en el VPS
git clone <tu-repo>.git /opt/signagetv
cd /opt/signagetv

# 2. Copiar y configurar variables de entorno
cp .env.example .env
nano .env   # poner secretos reales: MYSQL_PASSWORD, JWT_SECRET, PUBLIC_BASE_URL=https://signage.tu-dominio.com, CORS_ORIGINS

# 3. Levantar todo
docker compose up -d --build

# 4. Crear el primer local (entra al backend container)
docker exec -it signagetv-mysql mysql -uroot -p signagetv -e \
  "INSERT INTO locales (nombre, username, password_hash, created_at) VALUES ('Mi Local', 'milocal', '\$2a\$10\$...', NOW());"
# o crea una migración Flyway con tu seed inicial
```

### HTTPS (Let's Encrypt)

Recomendado para producción. Reemplaza el `nginx` del compose por una configuración con certbot, o pon Caddy delante. Ejemplo con Caddy en lugar de nginx:

```caddy
signage.tu-dominio.com {
  reverse_proxy /api/* backend:8080
  reverse_proxy /ws*   backend:8080
  reverse_proxy        admin:80
}
```

Una vez HTTPS esté activo, ajusta `PUBLIC_BASE_URL=https://signage.tu-dominio.com` y reinicia el backend (`docker compose restart backend`).

### Backup de datos

Los volúmenes a respaldar:
- `mysql_data` → dump SQL con `docker exec signagetv-mysql mysqldump ...`
- `media_data` → rsync del volumen al storage de tu elección

---

## Flujo de uso

1. **Admin** entra al panel, sube media (imágenes/videos), crea una playlist arrastrando items, configura duración y transición, opcionalmente crea un schedule "Desayuno L-V 7-11h".
2. **TV** se enciende, la app se abre automáticamente (es Leanback launcher), entra con el usuario del local y elige una playlist o "Automático según horario".
3. Cuando el admin guarda cualquier cambio, el backend publica en `/topic/local/{localId}/playlists` y todas las TVs del local **se actualizan al instante**.

---

## Stack técnico

| Capa | Tecnología |
|------|------------|
| Backend | Java 17, Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, Spring WebSocket (STOMP), Flyway |
| BD | MySQL 8 |
| Frontend | React 18, TypeScript, Vite, TailwindCSS, @hello-pangea/dnd, SockJS+Stomp |
| App TV | Kotlin, AndroidX, Leanback, Media3 ExoPlayer, Retrofit, OkHttp, Coil |
| Infra | Docker, Docker Compose, Nginx |

---

## Roadmap post-MVP

- Reportes de reproducción (qué se mostró y cuándo).
- Editor visual de plantillas (texto sobre imagen, ticker de noticias).
- Notificaciones push a TVs específicas (mensajes urgentes).
- Métricas de uso por playlist.
- Soporte multi-zona dentro de la misma pantalla.

---

## Licencia

MIT (ajustar según necesidad).
