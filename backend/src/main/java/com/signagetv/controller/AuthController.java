package com.signagetv.controller;

import com.signagetv.dto.LocalDto;
import com.signagetv.dto.LoginRequest;
import com.signagetv.dto.LoginResponse;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public LocalDto me() {
        return authService.me(SecurityUtils.currentLocalId());
    }
}
