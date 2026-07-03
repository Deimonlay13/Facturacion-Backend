package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ArchivoEntity;
import com.gdl.facturacion_backend.enums.TipoArchivo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchivoRepository extends BaseTenantRepository<ArchivoEntity> {

    /**
     * Busca archivos por documento y tipo.
     */
    @Query("""
            SELECT a FROM ArchivoEntity a
            WHERE a.documento.id = :documentoId
              AND a.tipo = :tipo
            ORDER BY a.id DESC
            """)
    List<ArchivoEntity> findByDocumentoIdAndTipo(@Param("documentoId") Long documentoId,
                                                  @Param("tipo") TipoArchivo tipo);

    /**
     * Busca el archivo más reciente de un tipo para un documento.
     */
    @Query("""
            SELECT a FROM ArchivoEntity a
            WHERE a.documento.id = :documentoId
              AND a.tipo = :tipo
            ORDER BY a.id DESC
            LIMIT 1
            """)
    Optional<ArchivoEntity> findLatestByDocumentoIdAndTipo(@Param("documentoId") Long documentoId,
                                                            @Param("tipo") TipoArchivo tipo);

    /**
     * Busca todos los archivos de un documento.
     */
    @Query("""
            SELECT a FROM ArchivoEntity a
            WHERE a.documento.id = :documentoId
            ORDER BY a.tipo, a.id DESC
            """)
    List<ArchivoEntity> findByDocumentoId(@Param("documentoId") Long documentoId);
}
