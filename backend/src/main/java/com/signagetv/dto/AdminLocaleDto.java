package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vista enriquecida de un Local (role=LOCAL) para el panel super-admin.
 * Incluye contadores agregados de contenido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLocaleDto {
    private Long id;
    private String nombre;
    private String username;
    private Boolean active;
    private LocalDateTime createdAt;
    private Long tvCount;
    private Long mediaCount;
    private Long playlistCount;
}
