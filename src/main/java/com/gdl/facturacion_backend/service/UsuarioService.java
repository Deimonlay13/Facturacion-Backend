package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioCreateRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioUpdateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String ROL_ADMIN = "ROLE_ADMIN";
    private static final String ROL_USER = "ROLE_USER";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmpresaService empresaService;
    private final RolService roleService;

    public AuthResponse register(RegisterRequest request) {

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ReglaNegocioException("El nombre de usuario ya está en uso");
        }

        Long empresaId = empresaIdActual();
        EmpresaEntity empresa = empresaService.findById(empresaId);

        // El rol NO se elige libremente desde el registro público:
        // el primer usuario de una empresa queda como ADMIN y el resto como USER.
        boolean esPrimerUsuario = usuarioRepository.findAllByEmpresaId(empresaId).isEmpty();
        String nombreRol = esPrimerUsuario ? ROL_ADMIN : ROL_USER;
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
                .orElseThrow(() -> new ReglaNegocioException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("Credenciales inválidas");
        }

        Long empresaId = usuario.getEmpresa().getId();

        String token = jwtService.generateToken(usuario.getUsername(), empresaId, usuario.getRol().getNombre());
        return new AuthResponse(token);
    }

    // ---------------------------------------------------------------------
    // Administración de usuarios (scoped a la empresa del solicitante)
    // ---------------------------------------------------------------------

    public List<UsuarioEntity> listar() {
        return usuarioRepository.findAllByEmpresaId(empresaIdActual());
    }

    public UsuarioEntity obtenerPorId(Long id) {
        return usuarioRepository.findByIdAndEmpresaId(id, empresaIdActual())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    public UsuarioEntity crear(UsuarioCreateRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ReglaNegocioException("El nombre de usuario ya está en uso");
        }

        Long empresaId = empresaIdActual();
        EmpresaEntity empresa = empresaService.findById(empresaId);

        String nombreRol = (request.getRol() != null && !request.getRol().isBlank())
                ? request.getRol() : ROL_USER;
        RolEntity rol = roleService.findByNombre(nombreRol);

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(request.getUsername());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuario.setEmpresa(empresa);
        usuario.setRol(rol);

        return usuarioRepository.save(usuario);
    }

    public UsuarioEntity actualizar(Long id, UsuarioUpdateRequest request) {
        UsuarioEntity usuario = obtenerPorId(id);

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(usuario.getUsername())) {
            if (usuarioRepository.existsByUsername(request.getUsername())) {
                throw new ReglaNegocioException("El nombre de usuario ya está en uso");
            }
            usuario.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return usuarioRepository.save(usuario);
    }

    public UsuarioEntity cambiarRol(Long id, String nombreRol) {
        UsuarioEntity usuario = obtenerPorId(id);
        RolEntity rol = roleService.findByNombre(nombreRol);
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    public UsuarioEntity cambiarEstado(Long id, boolean activo) {
        UsuarioEntity usuario = obtenerPorId(id);
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }

    private Long empresaIdActual() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new ReglaNegocioException(
                    "Operación no permitida: falta identificación de empresa (X-Tenant-ID o JWT)");
        }
        return empresaId;
    }
}
