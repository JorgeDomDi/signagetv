package com.signagetv.entity;

/**
 * Rol del usuario almacenado en la tabla {@code locales}.
 *
 * <ul>
 *   <li>{@link #LOCAL}: usuario asociado a una tienda física. Tiene contenido (media,
 *       playlists, schedules, TVs) y entra al panel normal.</li>
 *   <li>{@link #SUPER_ADMIN}: administrador del sistema. No tiene contenido propio,
 *       gestiona tiendas y otros super-admins.</li>
 * </ul>
 */
public enum Role {
    LOCAL,
    SUPER_ADMIN
}
