package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.enums.EstadoFolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolioRepository extends BaseTenantRepository<FolioEntity> {

    Page<FolioEntity> findByEmpresaId(Long empresaId, Pageable pageable);

    boolean existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(Integer codigoSii,
                                                                       Long empresaId,
                                                                       Integer desde,
                                                                       Integer hasta);

    long countByCafIdAndEmpresaId(Long cafId, Long empresaId);

    long countByCafIdAndEmpresaIdAndEstado(Long cafId, Long empresaId, EstadoFolio estado);

    long countByTipoDocumentoCodigoSiiAndEmpresaIdAndEstado(Integer codigoSii,
                                                            Long empresaId,
                                                            EstadoFolio estado);

    Optional<FolioEntity> findFirstByTipoDocumentoCodigoSiiAndEmpresaIdAndEstadoOrderByNumeroAsc(
            Integer codigoSii,
            Long empresaId,
            EstadoFolio estado);

    Optional<FolioEntity> findFirstByTipoDocumentoCodigoSiiAndEmpresaIdOrderByNumeroDesc(
            Integer codigoSii,
            Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FolioEntity> findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
            Long cafId,
            Long empresaId,
            EstadoFolio estado);
}
