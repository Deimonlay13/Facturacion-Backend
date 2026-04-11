package com.gdl.facturacion_backend.service;


import org.springframework.stereotype.Service;
import com.gdl.facturacion_backend.context.TenantContext;

@Service
public class TenantService {

    public Long getEmpresaId() {
        Long empresaId = TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new RuntimeException("No se encontró empresa en el contexto del tenant");
        }

        return empresaId;
    }
}