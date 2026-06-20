package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenciaDocumentoRepository
        extends BaseTenantRepository<ReferenciaDocumentoEntity> {
}
