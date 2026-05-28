package com.signagetv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminLocaleCreateRequest {
    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotBlank
    @Size(min = 3, max = 60)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
