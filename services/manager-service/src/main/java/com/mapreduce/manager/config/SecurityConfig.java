package com.mapreduce.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final WorkerTokenFilter workerTokenFilter;

    public SecurityConfig(WorkerTokenFilter workerTokenFilter) {
        this.workerTokenFilter = workerTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // These are protected by WorkerTokenFilter.
                        .requestMatchers("/internal/v1/**").permitAll()

                        // User-facing API requires a valid Keycloak JWT.
                        .requestMatchers("/api/v1/jobs/**").authenticated()

                        .anyRequest().denyAll()
                )
                .addFilterBefore(workerTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                );

        return http.build();
    }
}