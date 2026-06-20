package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.enums.EstadoFolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolioRepository extends BaseTenantRepository<FolioEntity> {

    boolean existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(Integer codigoSii,
                                                                       Long empresaId,
                                                                       Integer desde,
                                                                       Integer hasta);

    long countByCafIdAndEmpresaId(Long cafId, Long empresaId);

    long countByCafIdAndEmpresaIdAndEstado(Long cafId, Long empresaId, EstadoFolio estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FolioEntity> findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
            Long cafId,
            Long empresaId,
            EstadoFolio estado);
}
