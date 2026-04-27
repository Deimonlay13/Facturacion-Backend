package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/signup")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return usuarioService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }
}