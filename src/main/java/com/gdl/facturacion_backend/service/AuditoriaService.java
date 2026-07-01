package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.entity.AuditoriaEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.repository.AuditoriaRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Registra acciones de escritura en la tabla de auditoría.
 * Nunca debe romper la operación de negocio: cualquier error se ignora.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public void registrar(String tabla, String accion, String detalle) {
        try {
            AuditoriaEntity a = new AuditoriaEntity();
            a.setTabla(tabla);
            a.setAccion(accion);
            a.setDetalle(detalle);
            a.setFecha(LocalDateTime.now());
            a.setUsuarioId(usuarioIdActual());
            auditoriaRepository.save(a);
        } catch (Exception ignored) {
            // la auditoría no debe afectar el flujo principal
        }
    }

    private Long usuarioIdActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return usuarioRepository.findByUsername(auth.getName())
                        .map(UsuarioEntity::getId)
                        .orElse(null);
            }
        } catch (Exception ignored) {
            // sin contexto de seguridad (ej. tareas de arranque)
        }
        return null;
    }
}
