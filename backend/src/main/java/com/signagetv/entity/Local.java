package com.signagetv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Usuario del sistema. Histricamente representaba sólo una sucursal/cliente (rol
 * {@link Role#LOCAL}), pero ahora también soporta el rol {@link Role#SUPER_ADMIN}
 * para administradores globales. Por compatibilidad de datos, ambos viven en la
 * misma tabla {@code locales}.
 *
 * <p>Las consultas multi-tenant que listan/operan sobre contenido (media, playlists,
 * etc.) deben filtrar siempre {@code role = LOCAL}; los super-admins no poseen
 * contenido propio.</p>
 */
@Entity
@Table(name = "locales")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Role role;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (role == null) role = Role.LOCAL;
        if (active == null) active = Boolean.TRUE;
    }
}
