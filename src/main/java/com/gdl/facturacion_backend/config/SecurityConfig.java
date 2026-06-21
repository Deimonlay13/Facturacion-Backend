package com.gdl.facturacion_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import com.gdl.facturacion_backend.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // --- Público ---
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/admin/**", "/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/tipos-documento/**").permitAll()
                        // Crear empresa queda abierto para el bootstrap (sin empresa no hay token)
                        .requestMatchers(HttpMethod.POST, "/empresas").permitAll()

                        // --- Solo administradores ---
                        .requestMatchers("/empresas/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/usuarios/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/auditoria/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/api/folios/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN")

                        // --- Cualquier usuario autenticado (clientes, productos, documentos) ---
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                escribirJson(res, 401, "No autenticado", "Debes iniciar sesión"))
                        .accessDeniedHandler((req, res, e) ->
                                escribirJson(res, 403, "Acceso denegado", "No tienes permisos para esta operación")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Escribe una respuesta de error en JSON (mismo formato que ErrorResponse). */
    private static void escribirJson(HttpServletResponse res, int status, String error, String mensaje)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"mensaje\":\"%s\",\"timestamp\":\"%s\"}",
                status, error, mensaje, java.time.LocalDateTime.now()));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Tenant-ID"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
