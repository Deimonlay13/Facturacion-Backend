package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuiaDespachoExtraRepository
        extends BaseTenantRepository<GuiaDespachoExtraEntity> {

    Optional<GuiaDespachoExtraEntity> findByDocumentoIdAndEmpresaId(Long documentoId, Long empresaId);
}
