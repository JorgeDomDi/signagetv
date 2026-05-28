package com.signagetv.repository;

import com.signagetv.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
    List<MediaItem> findByLocalIdOrderByCreatedAtDesc(Long localId);
    Optional<MediaItem> findByIdAndLocalId(Long id, Long localId);
    long countByLocalId(Long localId);
}
