package com.gdl.facturacion_backend.entity;

import com.gdl.facturacion_backend.context.TenantContext;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    protected EmpresaEntity empresa;

    @Column(name = "created_at", updatable = false)
    protected OffsetDateTime createdAt;

    @Column(name = "updated_at")
    protected OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    protected OffsetDateTime deletedAt;

    @Column(name = "can_delete")
    protected Boolean canDelete = true;

    @PrePersist
    protected void onCreate() {

        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();

        if (empresa == null && requiereEmpresa()) {

            Long empresaId = TenantContext.getEmpresaId();

            if (empresaId == null) {
                throw new RuntimeException("No se encontró empresa en el contexto");
            }

            EmpresaEntity emp = new EmpresaEntity();
            emp.setId(empresaId);

            this.empresa = emp;
        }
    }

    protected boolean requiereEmpresa() {
        return true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
