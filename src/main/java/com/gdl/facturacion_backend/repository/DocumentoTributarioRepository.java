package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentoTributarioRepository
        extends BaseTenantRepository<DocumentoTributarioEntity> {

    /**
     * Consulta con filtros opcionales (null = ignorar). Siempre acotado a la empresa.
     */
    @Query("""
            SELECT d FROM DocumentoTributarioEntity d
            WHERE d.empresa.id = :empresaId
              AND (:clienteId IS NULL OR d.cliente.id = :clienteId)
              AND (:codigoTipo IS NULL OR d.tipoDocumento.codigoSii = :codigoTipo)
              AND (:estado IS NULL OR d.estado = :estado)
              AND (:desde IS NULL OR d.fechaEmision >= :desde)
              AND (:hasta IS NULL OR d.fechaEmision <= :hasta)
            ORDER BY d.id DESC
            """)
    List<DocumentoTributarioEntity> buscar(@Param("empresaId") Long empresaId,
                                           @Param("clienteId") Long clienteId,
                                           @Param("codigoTipo") Integer codigoTipo,
                                           @Param("estado") EstadoDocumento estado,
                                           @Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta);
}
