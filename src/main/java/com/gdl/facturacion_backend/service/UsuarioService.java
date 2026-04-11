package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmpresaService empresaService;

   public AuthResponse register(RegisterRequest request) {

    if (usuarioRepository.existsByUsername(request.getUsername())) {
        throw new RuntimeException("El usuario ya existe");
    }

    // Validar que la empresa del contexto existe en BD
    Long empresaId = TenantContext.getEmpresaId();
    if (empresaId == null) {
        throw new RuntimeException("Se requiere X-Tenant-ID para registrar un usuario");
    }

    empresaService.findById(empresaId); // valida que existe

    UsuarioEntity usuario = new UsuarioEntity();
    usuario.setUsername(request.getUsername());
    usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    usuario.setActivo(true);
    // BaseModelEntity#onCreate() asigna la empresa desde TenantContext automáticamente

    usuarioRepository.save(usuario);

    String token = jwtService.generateToken(usuario.getUsername(), empresaId);
    return new AuthResponse(token);
}
    public AuthResponse login(LoginRequest request) {

        UsuarioEntity usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Password incorrecta");
        }
        Long empresaId = usuario.getEmpresa().getId();
        String token = jwtService.generateToken(usuario.getUsername(), empresaId);
        return new AuthResponse(token);
    }
}
