package com.signagetv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tvs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Tv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "device_id", nullable = false, length = 80)
    private String deviceId;

    @Column(name = "current_playlist_id")
    private Long currentPlaylistId;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(nullable = false)
    @Builder.Default
    private Boolean online = false;
}
