package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.AuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, Long> {
    Optional<AuditoriaEntity> findFirstByTablaAndAccionAndDetalleOrderByFechaDesc(
            String tabla,
            String accion,
            String detalle);
}
