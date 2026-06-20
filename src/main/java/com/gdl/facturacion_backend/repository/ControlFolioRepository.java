package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ControlFolioRepository
        extends BaseTenantRepository<ControlFolioEntity> {

    Optional<ControlFolioEntity> findByTipoDocumentoCodigoSiiAndEmpresaId(Integer codigoSii, Long empresaId);
}
