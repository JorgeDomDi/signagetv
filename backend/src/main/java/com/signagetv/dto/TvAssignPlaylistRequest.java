package com.signagetv.dto;

import lombok.Data;

@Data
public class TvAssignPlaylistRequest {
    /** ID de la playlist a asignar; null para desasignar. */
    private Long playlistId;
}
