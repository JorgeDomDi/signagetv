package com.signagetv.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update parcial de una tienda. Los campos nulos no se modifican.
 * Para cambiar la contraseña usar {@link PasswordResetRequest} (endpoint aparte).
 */
@Data
public class AdminLocaleUpdateRequest {
    @Size(max = 120)
    private String nombre;
    private Boolean active;
}
