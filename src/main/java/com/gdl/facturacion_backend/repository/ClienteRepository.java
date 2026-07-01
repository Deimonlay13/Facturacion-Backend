package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends BaseTenantRepository<ClienteEntity> {

    Optional<ClienteEntity> findByRutAndEmpresaId(String rut, Long empresaId);

    boolean existsByRutAndEmpresaId(String rut, Long empresaId);

    /** Búsqueda usada por la importación TXT, que no trae RUT del cliente. */
    Optional<ClienteEntity> findFirstByEmpresaIdAndRazonSocialIgnoreCase(Long empresaId, String razonSocial);
}
