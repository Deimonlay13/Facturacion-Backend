package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Código 34 — Factura Exenta (IVA = 0, total = neto). */
@Component
public class ReglaFacturaExenta implements ReglaTributaria {

    @Override
    public Integer codigoSii() {
        return 34;
    }

    @Override
    public BigDecimal tasaIva() {
        return BigDecimal.ZERO;
    }

    @Override
    public void validarParaEmitir(DocumentoTributarioEntity documento) {
        if (documento.getDetalles() == null || documento.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La factura exenta debe tener al menos un detalle");
        }
    }
}
