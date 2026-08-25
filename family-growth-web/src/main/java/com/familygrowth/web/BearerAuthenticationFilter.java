package com.familygrowth.web;

import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage3Models;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class BearerAuthenticationFilter extends OncePerRequestFilter {
    private final Stage3Service service;

    BearerAuthenticationFilter(Stage3Service service) {
        this.service = service;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/")
            || "OPTIONS".equals(request.getMethod())
            || ("POST".equals(request.getMethod()) && (
                "/api/v1/auth/bootstrap".equals(path) || "/api/v1/auth/login".equals(path)));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        var actor = service.authenticate(token);
        if (actor.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"data\":null,\"error\":{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication required\"},\"traceId\":null}");
            return;
        }
        request.setAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE, actor.get());
        chain.doFilter(request, response);
    }
}
