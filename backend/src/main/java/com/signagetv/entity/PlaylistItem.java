package com.signagetv.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "playlist_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlaylistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "media_item_id", nullable = false)
    private Long mediaItemId;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
