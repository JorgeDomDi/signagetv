package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDto {
    private Long id;
    private String nombre;
    private String transicion;
    private Integer defaultImageSeconds;
    private LocalDateTime updatedAt;
    private List<PlaylistItemDto> items;
}
