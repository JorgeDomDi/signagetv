package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TvDto {
    private Long id;
    private String nombre;
    private String deviceId;
    private Long currentPlaylistId;
    private LocalDateTime lastSeen;
    private Boolean online;
}
