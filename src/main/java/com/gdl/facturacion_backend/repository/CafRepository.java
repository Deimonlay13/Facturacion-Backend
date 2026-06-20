package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.CafEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface CafRepository extends BaseTenantRepository<CafEntity> {
}
