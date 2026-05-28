package com.signagetv.controller;

import com.signagetv.dto.*;
import com.signagetv.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del panel super-admin. Toda la ruta {@code /api/v1/admin/**} requiere
 * autoridad {@code ROLE_SUPER_ADMIN} (configurado en {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ---- Tiendas (role=LOCAL) ----

    @GetMapping("/locales")
    public List<AdminLocaleDto> listLocales() {
        return adminService.listLocales();
    }

    @PostMapping("/locales")
    public AdminLocaleDto createLocale(@Valid @RequestBody AdminLocaleCreateRequest req) {
        return adminService.createLocale(req);
    }

    @PutMapping("/locales/{id}")
    public AdminLocaleDto updateLocale(@PathVariable Long id,
                                       @Valid @RequestBody AdminLocaleUpdateRequest req) {
        return adminService.updateLocale(id, req);
    }

    @PutMapping("/locales/{id}/password")
    public ResponseEntity<Void> resetLocalePassword(@PathVariable Long id,
                                                    @Valid @RequestBody PasswordResetRequest req) {
        adminService.resetLocalePassword(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/locales/{id}")
    public ResponseEntity<Void> deleteLocale(@PathVariable Long id) {
        adminService.deleteLocale(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Super-admins ----

    @GetMapping("/admins")
    public List<AdminUserDto> listAdmins() {
        return adminService.listAdmins();
    }

    @PostMapping("/admins")
    public AdminUserDto createAdmin(@Valid @RequestBody AdminUserCreateRequest req) {
        return adminService.createAdmin(req);
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
