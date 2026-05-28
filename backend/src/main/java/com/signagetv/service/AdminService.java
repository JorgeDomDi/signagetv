package com.signagetv.service;

import com.signagetv.dto.*;
import com.signagetv.entity.Local;
import com.signagetv.entity.MediaItem;
import com.signagetv.entity.Role;
import com.signagetv.exception.BadRequestException;
import com.signagetv.exception.NotFoundException;
import com.signagetv.repository.LocalRepository;
import com.signagetv.repository.MediaItemRepository;
import com.signagetv.repository.PlaylistRepository;
import com.signagetv.repository.TvRepository;
import com.signagetv.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que da soporte al panel super-admin: alta/baja/edición de tiendas
 * (role=LOCAL) y de otros super-admins (role=SUPER_ADMIN).
 *
 * <p>Todas las operaciones presuponen que el caller ya está autorizado como
 * SUPER_ADMIN (lo enforcea {@code SecurityConfig} a nivel de filter chain).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final LocalRepository localRepo;
    private final MediaItemRepository mediaRepo;
    private final PlaylistRepository playlistRepo;
    private final TvRepository tvRepo;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    // ============================================================
    //  TIENDAS (role = LOCAL)
    // ============================================================

    public List<AdminLocaleDto> listLocales() {
        return localRepo.findByRoleOrderByNombreAsc(Role.LOCAL).stream()
                .map(this::toLocaleDto)
                .toList();
    }

    @Transactional
    public AdminLocaleDto createLocale(AdminLocaleCreateRequest req) {
        if (localRepo.existsByUsername(req.getUsername())) {
            throw new BadRequestException("El usuario '" + req.getUsername() + "' ya existe");
        }
        Local l = Local.builder()
                .nombre(req.getNombre().trim())
                .username(req.getUsername().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.LOCAL)
                .active(Boolean.TRUE)
                .build();
        l = localRepo.save(l);
        log.info("Admin: creada tienda id={} username={}", l.getId(), l.getUsername());
        return toLocaleDto(l);
    }

    @Transactional
    public AdminLocaleDto updateLocale(Long id, AdminLocaleUpdateRequest req) {
        Local l = localRepo.findByIdAndRole(id, Role.LOCAL)
                .orElseThrow(() -> new NotFoundException("Tienda no encontrada"));
        if (req.getNombre() != null && !req.getNombre().isBlank()) {
            l.setNombre(req.getNombre().trim());
        }
        if (req.getActive() != null) {
            l.setActive(req.getActive());
        }
        l = localRepo.save(l);
        return toLocaleDto(l);
    }

    @Transactional
    public void resetLocalePassword(Long id, PasswordResetRequest req) {
        Local l = localRepo.findByIdAndRole(id, Role.LOCAL)
                .orElseThrow(() -> new NotFoundException("Tienda no encontrada"));
        l.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        localRepo.save(l);
        log.info("Admin: contraseña reseteada para tienda id={} username={}", l.getId(), l.getUsername());
    }

    /**
     * Elimina la tienda y todo su contenido. Los FK con ON DELETE CASCADE en la
     * BD borran las filas hijas (media_items, playlists, playlist_items, schedules,
     * tvs); aquí nos encargamos antes de borrar los archivos físicos de disco.
     */
    @Transactional
    public void deleteLocale(Long id) {
        Local l = localRepo.findByIdAndRole(id, Role.LOCAL)
                .orElseThrow(() -> new NotFoundException("Tienda no encontrada"));

        // Borrar archivos físicos de media antes de que el cascade elimine las filas
        List<MediaItem> mediaItems = mediaRepo.findByLocalIdOrderByCreatedAtDesc(l.getId());
        for (MediaItem mi : mediaItems) {
            storageService.delete(mi.getStoragePath());
        }

        localRepo.delete(l);
        log.warn("Admin: eliminada tienda id={} username={} ({} media borrados)",
                l.getId(), l.getUsername(), mediaItems.size());
    }

    // ============================================================
    //  SUPER-ADMINS (role = SUPER_ADMIN)
    // ============================================================

    public List<AdminUserDto> listAdmins() {
        return localRepo.findByRoleOrderByNombreAsc(Role.SUPER_ADMIN).stream()
                .map(this::toAdminUserDto)
                .toList();
    }

    @Transactional
    public AdminUserDto createAdmin(AdminUserCreateRequest req) {
        if (localRepo.existsByUsername(req.getUsername())) {
            throw new BadRequestException("El usuario '" + req.getUsername() + "' ya existe");
        }
        Local a = Local.builder()
                .nombre(req.getNombre().trim())
                .username(req.getUsername().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.SUPER_ADMIN)
                .active(Boolean.TRUE)
                .build();
        a = localRepo.save(a);
        log.info("Admin: creado super-admin id={} username={}", a.getId(), a.getUsername());
        return toAdminUserDto(a);
    }

    @Transactional
    public void deleteAdmin(Long id) {
        Long currentId = SecurityUtils.currentLocalId();
        if (currentId.equals(id)) {
            throw new BadRequestException("No puedes eliminar tu propia cuenta de super-admin");
        }
        Local a = localRepo.findByIdAndRole(id, Role.SUPER_ADMIN)
                .orElseThrow(() -> new NotFoundException("Super-admin no encontrado"));

        // Evita dejar el sistema sin ningún admin activo
        long activeAdmins = localRepo.countByRoleAndActiveTrue(Role.SUPER_ADMIN);
        boolean targetIsActive = Boolean.TRUE.equals(a.getActive());
        if (targetIsActive && activeAdmins <= 1) {
            throw new BadRequestException("No se puede eliminar el último super-admin activo");
        }

        localRepo.delete(a);
        log.warn("Admin: eliminado super-admin id={} username={}", a.getId(), a.getUsername());
    }

    // ============================================================
    //  Mapeos
    // ============================================================

    private AdminLocaleDto toLocaleDto(Local l) {
        return AdminLocaleDto.builder()
                .id(l.getId())
                .nombre(l.getNombre())
                .username(l.getUsername())
                .active(l.getActive() != null ? l.getActive() : Boolean.TRUE)
                .createdAt(l.getCreatedAt())
                .tvCount(tvRepo.countByLocalId(l.getId()))
                .mediaCount(mediaRepo.countByLocalId(l.getId()))
                .playlistCount(playlistRepo.countByLocalId(l.getId()))
                .build();
    }

    private AdminUserDto toAdminUserDto(Local l) {
        return AdminUserDto.builder()
                .id(l.getId())
                .nombre(l.getNombre())
                .username(l.getUsername())
                .active(l.getActive() != null ? l.getActive() : Boolean.TRUE)
                .createdAt(l.getCreatedAt())
                .build();
    }
}
