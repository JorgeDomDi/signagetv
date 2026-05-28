package com.signagetv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ScheduleRequest {
    @NotNull
    private Long playlistId;
    @NotBlank
    private String nombre;
    @NotBlank
    private String diasSemana;
    @NotNull
    private LocalTime horaInicio;
    @NotNull
    private LocalTime horaFin;
    private Boolean activo;
    private Integer prioridad;
}
