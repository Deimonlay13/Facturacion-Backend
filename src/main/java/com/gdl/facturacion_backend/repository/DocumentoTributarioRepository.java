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
     * Consulta con filtros opcionales (null = ignorar). empresaId null = todas las empresas
     * (solo aplica a SUPER_ADMIN); en caso contrario queda acotado a la empresa indicada.
     */
    @Query("""
            SELECT DISTINCT d FROM DocumentoTributarioEntity d
            LEFT JOIN FETCH d.usuarioEmisor
            LEFT JOIN FETCH d.tipoDocumento
            LEFT JOIN FETCH d.cliente
            WHERE (:empresaId IS NULL OR d.empresa.id = :empresaId)
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

    @Query("""
            SELECT u.id, COALESCE(u.username, d.nombreUsuarioEmisor, 'Sin usuario'), COUNT(d), COALESCE(SUM(d.montoTotal), 0)
            FROM DocumentoTributarioEntity d
            LEFT JOIN d.usuarioEmisor u
            WHERE (:empresaId IS NULL OR d.empresa.id = :empresaId)
              AND d.estado = com.gdl.facturacion_backend.enums.EstadoDocumento.EMITIDO
              AND (:codigoTipo IS NULL OR d.tipoDocumento.codigoSii = :codigoTipo)
              AND (:desde IS NULL OR d.fechaEmision >= :desde)
              AND (:hasta IS NULL OR d.fechaEmision <= :hasta)
            GROUP BY u.id, u.username, d.nombreUsuarioEmisor
            ORDER BY COUNT(d) DESC, COALESCE(SUM(d.montoTotal), 0) DESC
            """)
    List<Object[]> estadisticasPorEmisor(@Param("empresaId") Long empresaId,
                                         @Param("codigoTipo") Integer codigoTipo,
                                         @Param("desde") LocalDate desde,
                                         @Param("hasta") LocalDate hasta);
}
