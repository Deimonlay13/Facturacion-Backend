package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;

import java.math.BigDecimal;

/**
 * Base para notas (56/61): exigen al menos una referencia con documento y motivo.
 */
public abstract class ReglaNotaBase implements ReglaTributaria {

    @Override
    public BigDecimal tasaIva() {
        return new BigDecimal("0.19");
    }

    @Override
    public void validarParaEmitir(DocumentoTributarioEntity documento) {
        if (documento.getReferencias() == null || documento.getReferencias().isEmpty()) {
            throw new ReglaNegocioException("La nota debe referenciar un documento existente");
        }
        boolean tieneReferenciaValida = documento.getReferencias().stream()
                .anyMatch(this::referenciaValida);
        if (!tieneReferenciaValida) {
            throw new ReglaNegocioException("La referencia debe indicar el documento referenciado y el motivo");
        }
    }

    private boolean referenciaValida(ReferenciaDocumentoEntity ref) {
        return ref.getDocumentoReferenciado() != null
                && ref.getMotivo() != null
                && !ref.getMotivo().isBlank();
    }
}
