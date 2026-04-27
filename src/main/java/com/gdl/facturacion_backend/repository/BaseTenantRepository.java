package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.BaseModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseTenantRepository<T extends BaseModelEntity> extends JpaRepository<T, Long> {
  
    List<T> findAllByEmpresaId(Long empresaId);

    Optional<T> findByIdAndEmpresaId(Long id, Long empresaId);
}