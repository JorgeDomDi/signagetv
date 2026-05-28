package com.signagetv.repository;

import com.signagetv.entity.Tv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TvRepository extends JpaRepository<Tv, Long> {
    List<Tv> findByLocalIdOrderByNombreAsc(Long localId);
    Optional<Tv> findByIdAndLocalId(Long id, Long localId);
    Optional<Tv> findByLocalIdAndDeviceId(Long localId, String deviceId);
    long countByLocalId(Long localId);
}
