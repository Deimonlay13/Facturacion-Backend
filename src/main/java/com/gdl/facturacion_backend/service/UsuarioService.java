package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.RolEntity;
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
    private final RolService roleService;

    public AuthResponse register(RegisterRequest request) {

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new RuntimeException("Operación no permitida: Falta identificación de empresa");
        }

        EmpresaEntity empresa = empresaService.findById(empresaId);

        String nombreRol = (request.getRol() != null) ? request.getRol() : "ROLE_USER";
        RolEntity role = roleService.findByNombre(nombreRol);

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(request.getUsername());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuario.setEmpresa(empresa);
        
        usuario.setRol(role);

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getUsername(), empresaId, role.getNombre());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        Long empresaId = usuario.getEmpresa().getId();

        String token = jwtService.generateToken(usuario.getUsername(), empresaId, usuario.getRol().getNombre());
        return new AuthResponse(token);
    }
}