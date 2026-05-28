package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vista de un super-admin (role=SUPER_ADMIN) en el panel de gestión.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String nombre;
    private String username;
    private Boolean active;
    private LocalDateTime createdAt;
}
