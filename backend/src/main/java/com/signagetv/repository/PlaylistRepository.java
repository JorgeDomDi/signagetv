package com.signagetv.repository;

import com.signagetv.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByLocalIdOrderByNombreAsc(Long localId);
    Optional<Playlist> findByIdAndLocalId(Long id, Long localId);
    long countByLocalId(Long localId);
}
