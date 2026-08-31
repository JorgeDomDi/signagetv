package com.signagetv.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Pista de musica de fondo de una playlist.
 *
 * <p>Deliberadamente separada de {@link PlaylistItem}: el audio no forma parte de
 * la rotacion visual. La app de TV reproduce estas pistas en un reproductor propio,
 * en orden y en loop infinito, mientras las imagenes y videos siguen su ciclo.</p>
 */
@Entity
@Table(name = "playlist_audio_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlaylistAudioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "media_item_id", nullable = false)
    private Long mediaItemId;

    @Column(nullable = false)
    private Integer position;
}
