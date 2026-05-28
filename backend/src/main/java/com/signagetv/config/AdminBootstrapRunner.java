package com.signagetv.config;

import com.signagetv.entity.Local;
import com.signagetv.entity.Role;
import com.signagetv.repository.LocalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inserta el super-admin inicial si no existe. Se ejecuta en TODOS los perfiles
 * (a diferencia de {@link SeedDataRunner} que sólo corre en {@code dev}) para
 * asegurar que siempre hay al menos un super-admin para administrar el sistema.
 *
 * <p>Credenciales seed:
 * <ul>
 *   <li>Usuario: {@code Superadmin}</li>
 *   <li>Contraseña: {@code Etruria69&#}</li>
 * </ul>
 * Recomendado cambiar la contraseña desde el panel tras el primer login.</p>
 */
@Slf4j
@Component
@Order(1) // Antes que SeedDataRunner por claridad de logs
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final String DEFAULT_USERNAME = "Superadmin";
    private static final String DEFAULT_PASSWORD = "Etruria69&#";
    private static final String DEFAULT_NOMBRE   = "Sistema";

    private final LocalRepository localRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (localRepo.existsByUsername(DEFAULT_USERNAME)) {
            log.info("Bootstrap: super-admin '{}' ya existe", DEFAULT_USERNAME);
            return;
        }
        Local admin = Local.builder()
                .username(DEFAULT_USERNAME)
                .nombre(DEFAULT_NOMBRE)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .role(Role.SUPER_ADMIN)
                .active(Boolean.TRUE)
                .build();
        localRepo.save(admin);
        log.warn("Bootstrap: creado super-admin inicial '{}' — cambia su contraseña cuanto antes",
                DEFAULT_USERNAME);
    }
}
