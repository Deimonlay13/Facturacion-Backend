package com.gdl.facturacion_backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
    
        return path.startsWith("/auth")
                || path.startsWith("/api/tipos-documento");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);
                String username = jwtService.extractUsername(token);
                Long empresaId = jwtService.extractEmpresaId(token);
                String rol = jwtService.extractRol(token);

                // Setear tenant para lecturas y escrituras
                if (empresaId != null) {
                    TenantContext.setEmpresaId(empresaId);
                }

                // El rol del JWT se usa como authority de Spring Security
                List<GrantedAuthority> authorities = (rol != null)
                        ? List.of(new SimpleGrantedAuthority(rol))
                        : List.of();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);

        } finally {
            // Limpiar siempre para evitar leaks entre requests
            TenantContext.clear();
        }
    }
    
}