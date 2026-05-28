package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalDto {
    private Long id;
    private String nombre;
    private String username;
    private String role;     // "LOCAL" | "SUPER_ADMIN"
    private Boolean active;
}
