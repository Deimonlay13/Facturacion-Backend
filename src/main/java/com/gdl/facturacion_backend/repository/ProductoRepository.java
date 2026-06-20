package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ProductoEntity;
import org.springframework.stereotype.Repository;

/**
 * Tenant-safe. Reemplaza al ProductoRepository de la rama `producto`
 * (que extendía JpaRepository sin aislamiento por empresa).
 */
@Repository
public interface ProductoRepository extends BaseTenantRepository<ProductoEntity> {
}
