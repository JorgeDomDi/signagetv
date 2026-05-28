package com.signagetv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PlaylistCreateRequest {
    @NotBlank
    private String nombre;
    private String transicion;          // opcional, default FADE
    @Positive
    private Integer defaultImageSeconds; // opcional
}
