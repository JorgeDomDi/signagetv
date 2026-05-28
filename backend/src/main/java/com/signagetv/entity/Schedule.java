package com.signagetv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Regla horaria que activa una Playlist en un rango (días + hora_inicio - hora_fin).
 * dias_semana se guarda como CSV: "MON,TUE,WED,THU,FRI"
 */
@Entity
@Table(name = "schedules")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "dias_semana", nullable = false, length = 40)
    private String diasSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer prioridad = 0;
}
