package com.gdl.facturacion_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gdl.facturacion_backend.entity.ProductoEntity;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Long>{

    boolean existsByCodigo(String codigo);

    Optional<ProductoEntity> findByCodigo(String codigo);
}
