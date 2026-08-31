package com.signagetv.repository;

import com.signagetv.entity.PlaylistAudioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistAudioItemRepository extends JpaRepository<PlaylistAudioItem, Long> {

    List<PlaylistAudioItem> findByPlaylistIdOrderByPositionAsc(Long playlistId);

    void deleteByPlaylistId(Long playlistId);
}
