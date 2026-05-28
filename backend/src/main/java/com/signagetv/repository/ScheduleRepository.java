package com.signagetv.repository;

import com.signagetv.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByLocalIdOrderByPrioridadDescIdAsc(Long localId);
    List<Schedule> findByLocalIdAndActivoTrue(Long localId);
    Optional<Schedule> findByIdAndLocalId(Long id, Long localId);
}
