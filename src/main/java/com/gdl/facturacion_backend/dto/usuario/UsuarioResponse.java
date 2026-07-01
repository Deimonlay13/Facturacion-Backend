package com.gdl.facturacion_backend.dto.usuario;

import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class UsuarioResponse {
    Long id;
    String username;
    Boolean activo;
    String rol;          // nombre técnico, ej: ROLE_ADMIN
    String rolMostrar;   // nombre amigable para UI
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    public static UsuarioResponse from(UsuarioEntity u) {
        RolEntity rol = u.getRol();
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getActivo(),
                rol != null ? rol.getNombre() : null,
                rol != null ? rol.getNombreMostrar() : null,
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
