package com.signagetv.repository;

import com.signagetv.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
    List<PlaylistItem> findByPlaylistIdOrderByPositionAsc(Long playlistId);
    void deleteByPlaylistId(Long playlistId);
}
