package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.EnvioSiiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioSiiRepository extends JpaRepository<EnvioSiiEntity, Long> {
    Optional<EnvioSiiEntity> findByTrackId(String trackId);

    List<EnvioSiiEntity> findAllByEmpresaId(Long empresaId);
}
