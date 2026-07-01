package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Código 33 — Factura Electrónica (afecta a IVA). */
@Component
public class ReglaFactura implements ReglaTributaria {

    @Override
    public Integer codigoSii() {
        return 33;
    }

    @Override
    public BigDecimal tasaIva() {
        return new BigDecimal("0.19");
    }

    @Override
    public void validarParaEmitir(DocumentoTributarioEntity documento) {
        if (documento.getDetalles() == null || documento.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La factura debe tener al menos un detalle");
        }
    }
}
