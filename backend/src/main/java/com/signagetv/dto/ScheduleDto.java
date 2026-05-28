package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDto {
    private Long id;
    private Long playlistId;
    private String nombre;
    private String diasSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean activo;
    private Integer prioridad;
}
