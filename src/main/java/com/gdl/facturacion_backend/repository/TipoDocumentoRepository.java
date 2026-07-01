package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumentoEntity, Long> {

    Optional<TipoDocumentoEntity> findByCodigoSii(Integer codigoSii);

    boolean existsByCodigoSii(Integer codigoSii);
}