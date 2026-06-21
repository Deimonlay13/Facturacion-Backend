package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RefreshRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.service.RefreshTokenService;
import com.gdl.facturacion_backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Autenticación", description = "Login, registro, refresh y logout")
public class AuthController {

    private final UsuarioService usuarioService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/signup")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return usuarioService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return refreshTokenService.refrescar(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshRequest request) {
        refreshTokenService.revocar(request.getRefreshToken());
    }
}
