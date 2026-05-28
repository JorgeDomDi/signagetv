package com.signagetv.service;

import com.signagetv.dto.LocalDto;
import com.signagetv.dto.LoginRequest;
import com.signagetv.dto.LoginResponse;
import com.signagetv.entity.Local;
import com.signagetv.exception.NotFoundException;
import com.signagetv.exception.UnauthorizedException;
import com.signagetv.repository.LocalRepository;
import com.signagetv.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final LocalRepository localRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest req) {
        Local local = localRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(req.getPassword(), local.getPasswordHash())) {
            throw new UnauthorizedException("Usuario o contraseña incorrectos");
        }

        // Cuentas suspendidas: rechazo explícito (HTTP 403)
        if (Boolean.FALSE.equals(local.getActive())) {
            log.warn("Login bloqueado: cuenta suspendida username={}", local.getUsername());
            throw new org.springframework.security.access.AccessDeniedException("Cuenta suspendida");
        }

        String roleName = local.getRole() != null ? local.getRole().name() : "LOCAL";
        String token = jwtService.generate(local.getId(), local.getUsername(), roleName);
        log.info("Login OK local={} ({}) role={}", local.getId(), local.getUsername(), roleName);

        return LoginResponse.builder()
                .token(token)
                .local(toDto(local))
                .build();
    }

    public LocalDto me(Long localId) {
        Local local = localRepo.findById(localId)
                .orElseThrow(() -> new NotFoundException("Local no encontrado"));
        return toDto(local);
    }

    private LocalDto toDto(Local l) {
        return LocalDto.builder()
                .id(l.getId())
                .nombre(l.getNombre())
                .username(l.getUsername())
                .role(l.getRole() != null ? l.getRole().name() : "LOCAL")
                .active(l.getActive() != null ? l.getActive() : Boolean.TRUE)
                .build();
    }
}
