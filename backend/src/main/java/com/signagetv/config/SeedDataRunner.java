package com.signagetv.config;

import com.signagetv.entity.Local;
import com.signagetv.entity.Role;
import com.signagetv.repository.LocalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inserta 2 locales de prueba al arrancar en perfil dev si no existen ya.
 *  - local1 / password123
 *  - local2 / password123
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final LocalRepository localRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("local1", "Sucursal Centro", "password123");
        seed("local2", "Sucursal Norte",  "password123");
    }

    private void seed(String username, String nombre, String password) {
        if (localRepo.existsByUsername(username)) {
            log.info("Seed: local '{}' ya existe", username);
            return;
        }
        Local l = Local.builder()
                .username(username)
                .nombre(nombre)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.LOCAL)
                .active(Boolean.TRUE)
                .build();
        localRepo.save(l);
        log.info("Seed: creado local '{}' (id={})", username, l.getId());
    }
}
