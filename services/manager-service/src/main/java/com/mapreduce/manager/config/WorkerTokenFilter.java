package com.mapreduce.manager.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class WorkerTokenFilter extends OncePerRequestFilter {

    private static final String WORKER_TOKEN_HEADER = "X-Worker-Token";

    @Value("${manager.worker-auth-token:}")
    private String expectedWorkerToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/internal/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader(WORKER_TOKEN_HEADER);

        if (!isValidToken(providedToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Invalid worker token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String providedToken) {
        if (expectedWorkerToken == null || expectedWorkerToken.isBlank()) {
            return false;
        }

        if (providedToken == null || providedToken.isBlank()) {
            return false;
        }

        byte[] expected = expectedWorkerToken.getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedToken.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, provided);
    }
}