package com.signagetv.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Construye URLs absolutas para los recursos (media) sirviéndose de
 * `app.public-base-url` si está configurada, o del request actual en su defecto.
 */
@Component
public class UrlBuilder {

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    public String mediaFileUrl(Long mediaItemId) {
        String base = resolveBase();
        return base + "/api/v1/media/" + mediaItemId + "/file";
    }

    private String resolveBase() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return stripTrailingSlash(publicBaseUrl);
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "";
        HttpServletRequest req = attrs.getRequest();
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String portPart =
                ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)
                        ? ""
                        : ":" + port;
        return scheme + "://" + host + portPart;
    }

    private String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
