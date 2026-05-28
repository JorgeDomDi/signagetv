package com.signagetv.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return up;
    }

    public static Long currentLocalId() {
        return current().getLocalId();
    }
}
