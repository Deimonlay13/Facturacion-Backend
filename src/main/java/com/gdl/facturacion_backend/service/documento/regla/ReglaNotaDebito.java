package com.gdl.facturacion_backend.service.documento.regla;

import org.springframework.stereotype.Component;

/** Código 56 — Nota de Débito (requiere referencia a un documento existente). */
@Component
public class ReglaNotaDebito extends ReglaNotaBase {

    @Override
    public Integer codigoSii() {
        return 56;
    }
}
