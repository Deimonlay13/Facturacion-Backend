package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class UsuarioEntity extends BaseModelEntity {

    private static final String ROL_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private Boolean activo;

    @ManyToOne
    @JoinColumn(name = "id_rol")
    private RolEntity rol;

    @Override
    protected boolean requiereEmpresa() {
        return rol == null || !ROL_SUPER_ADMIN.equals(rol.getNombre());
    }
}
