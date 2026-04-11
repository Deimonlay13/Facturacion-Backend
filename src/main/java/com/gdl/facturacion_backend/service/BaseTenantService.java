package com.gdl.facturacion_backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gdl.facturacion_backend.entity.BaseModelEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;

public abstract class BaseTenantService<T extends BaseModelEntity> {

    protected final JpaRepository<T, Long> repository;
    protected final TenantService tenantService;

    protected BaseTenantService(
            JpaRepository<T, Long> repository,
            TenantService tenantService) {
        this.repository = repository;
        this.tenantService = tenantService;
    }

    protected Long getEmpresaId() {
        return tenantService.getEmpresaId();
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public Optional<T> findById(Long id) {
        return repository.findById(id);
    }

    public T save(T entity) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(getEmpresaId());
        entity.setEmpresa(empresa);

        return repository.save(entity);
    }
}