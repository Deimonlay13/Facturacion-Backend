package com.gdl.facturacion_backend.service.documento.regla;

import org.springframework.stereotype.Component;

/** Código 61 — Nota de Crédito (requiere referencia a un documento existente). */
@Component
public class ReglaNotaCredito extends ReglaNotaBase {

    @Override
    public Integer codigoSii() {
        return 61;
    }
}
