package com.gdl.facturacion_backend.service;

import java.util.List;
import java.util.Optional;
import com.gdl.facturacion_backend.entity.BaseModelEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.repository.BaseTenantRepository;
import com.gdl.facturacion_backend.security.SecurityUtils;

public abstract class BaseTenantService<T extends BaseModelEntity> {

    protected final BaseTenantRepository<T> repository;
    protected final TenantService tenantService;

    protected BaseTenantService(
            BaseTenantRepository<T> repository,
            TenantService tenantService) {
        this.repository = repository;
        this.tenantService = tenantService;
    }

    protected Long getEmpresaId() {
        return tenantService.getEmpresaId();
    }

    /**
     * Vista global: SUPER_ADMIN sin empresa seleccionada ve todas las empresas.
     * Si elige una empresa (X-Tenant-ID) queda acotado a ella, como cualquier usuario.
     */
    protected boolean vistaGlobal() {
        return SecurityUtils.esSuperAdminSinEmpresa();
    }

    public List<T> findAll() {
        return vistaGlobal()
                ? repository.findAll()
                : repository.findAllByEmpresaId(getEmpresaId());
    }

    public Optional<T> findById(Long id) {
        return vistaGlobal()
                ? repository.findById(id)
                : repository.findByIdAndEmpresaId(id, getEmpresaId());
    }

    public T save(T entity) {
        if (entity.getId() != null) {
            this.findById(entity.getId())
                    .orElseThrow(() -> new RuntimeException("No tienes permiso sobre este registro"));
        }

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(getEmpresaId());
        entity.setEmpresa(empresa);

        return repository.save(entity);
    }
}