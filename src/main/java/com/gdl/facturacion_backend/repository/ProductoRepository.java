package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ProductoEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends BaseTenantRepository<ProductoEntity> {
}
