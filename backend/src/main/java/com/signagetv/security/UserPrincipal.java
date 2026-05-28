package com.signagetv.security;

import com.signagetv.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Principal almacenado en el SecurityContext tras autenticar.
 *
 * <p>Para usuarios LOCAL representa al local autenticado (aislamiento multi-tenant
 * por {@code localId}). Para SUPER_ADMIN, {@code localId} es el id de la fila del
 * super-admin en la tabla {@code locales} pero NO debe usarse como identificador
 * de tienda — los super-admins no poseen contenido.</p>
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {
    private final Long localId;
    private final String username;
    private final Role role;
}
