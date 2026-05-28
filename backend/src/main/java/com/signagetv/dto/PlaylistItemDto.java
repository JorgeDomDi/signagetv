package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemDto {
    private Long id;
    private Integer position;
    private Integer durationSeconds;
    private MediaItemDto media;
}
