package com.signagetv.security;

import com.signagetv.entity.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que extrae el JWT del header Authorization y monta el UserPrincipal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                Long localId = claims.get("localId", Number.class).longValue();
                String username = claims.get("username", String.class);
                String roleStr = claims.get("role", String.class);

                // Tokens emitidos antes de añadir el claim 'role' se tratan como LOCAL
                Role role;
                try {
                    role = roleStr != null ? Role.valueOf(roleStr) : Role.LOCAL;
                } catch (IllegalArgumentException ex) {
                    role = Role.LOCAL;
                }

                UserPrincipal principal = new UserPrincipal(localId, username, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                log.debug("JWT inválido: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Extrae el JWT, primero del header Authorization (modo estándar para clientes
     * que pueden enviar headers — el panel admin con axios, la app Android con OkHttp).
     *
     * Como fallback, acepta el token vía query param {@code ?token=...} SOLO para el
     * endpoint de descarga binaria de media ({@code GET /api/v1/media/{id}/file}),
     * donde los tags HTML {@code <img>} y {@code <video>} no pueden añadir headers.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // Fallback por query param sólo para descarga de archivos de media.
        String path = request.getRequestURI();
        if (path != null
                && path.startsWith("/api/v1/media/")
                && path.endsWith("/file")
                && "GET".equalsIgnoreCase(request.getMethod())) {
            String q = request.getParameter("token");
            if (q != null && !q.isBlank()) return q;
        }
        return null;
    }
}
