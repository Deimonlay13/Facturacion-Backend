package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.UsuarioEntity;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends BaseTenantRepository<UsuarioEntity> {

    Optional<UsuarioEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    List<UsuarioEntity> findAllByRolNombre(String nombreRol);
}
