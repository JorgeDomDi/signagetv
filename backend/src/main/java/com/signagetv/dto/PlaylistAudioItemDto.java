package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una pista de musica de fondo dentro de una playlist. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistAudioItemDto {
    private Long id;
    private Integer position;
    private MediaItemDto media;
}
