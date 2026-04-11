package com.gdl.facturacion_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gdl.facturacion_backend.entity.EmpresaEntity;

public interface EmpresaRepository extends JpaRepository<EmpresaEntity, Long> {
    Optional<EmpresaEntity> findByRutEmpresa(String rutEmpresa);
}