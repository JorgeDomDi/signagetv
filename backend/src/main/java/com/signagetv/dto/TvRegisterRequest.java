package com.signagetv.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TvRegisterRequest {
    @NotBlank
    private String deviceId;
    @NotBlank
    private String nombre;
}
