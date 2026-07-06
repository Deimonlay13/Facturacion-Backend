package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.HistorialEstadoSiiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoSiiRepository extends JpaRepository<HistorialEstadoSiiEntity, Long> {
    List<HistorialEstadoSiiEntity> findByDocumentoIdOrderByFechaEstadoAsc(Long documentoId);

    List<HistorialEstadoSiiEntity> findAllByEmpresaId(Long empresaId);
}
