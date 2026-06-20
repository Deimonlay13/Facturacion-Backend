package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import lombok.Value;

@Value
public class ReferenciaResponse {
    Long id;
    Long documentoReferenciadoId;
    Integer folioReferenciado;
    String tipoReferencia;
    String motivo;

    public static ReferenciaResponse from(ReferenciaDocumentoEntity e) {
        DocumentoTributarioEntity ref = e.getDocumentoReferenciado();
        return new ReferenciaResponse(
                e.getId(),
                ref != null ? ref.getId() : null,
                ref != null ? ref.getFolio() : null,
                e.getTipoReferencia(),
                e.getMotivo()
        );
    }
}
