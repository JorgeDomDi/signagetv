package com.signagetv.dto;

import lombok.Data;

import java.util.List;

/**
 * Cuerpo de PUT /api/v1/playlists/{id}/audio-items.
 * Reemplaza por completo la lista de pistas de musica de fondo.
 * Una lista vacia deja la playlist sin musica.
 */
@Data
public class PlaylistAudioItemsReplaceRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private Long mediaItemId;
        private Integer position;
    }
}
