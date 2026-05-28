-- ============================================================
-- SignageTV - V2: rol + estado activo/suspendido en `locales`
-- ============================================================
--
-- Añade soporte para super-admins reutilizando la tabla `locales`.
-- Las filas existentes mantienen role='LOCAL' y active=TRUE (default),
-- por lo que las tiendas creadas con V1 siguen funcionando sin cambios.
--
-- La inserción del super-admin inicial NO se hace aquí (depende del
-- BCryptPasswordEncoder de Spring) — se hace en AdminBootstrapRunner,
-- que es idempotente y se ejecuta en todos los perfiles al arrancar.

ALTER TABLE locales
    ADD COLUMN role   VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER password_hash,
    ADD COLUMN active BOOLEAN     NOT NULL DEFAULT TRUE    AFTER role;

CREATE INDEX idx_locales_role ON locales (role);
