package com.signagetv.repository;

import com.signagetv.entity.Local;
import com.signagetv.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalRepository extends JpaRepository<Local, Long> {
    Optional<Local> findByUsername(String username);
    boolean existsByUsername(String username);

    List<Local> findByRoleOrderByNombreAsc(Role role);
    Optional<Local> findByIdAndRole(Long id, Role role);
    long countByRoleAndActiveTrue(Role role);
}
