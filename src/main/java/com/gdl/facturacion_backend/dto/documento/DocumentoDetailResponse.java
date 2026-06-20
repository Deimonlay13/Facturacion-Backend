package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import lombok.Value;

import java.util.List;

@Value
public class DocumentoDetailResponse {
    DocumentoResponse documento;
    List<DetalleResponse> detalles;
    List<ReferenciaResponse> referencias;
    GuiaDespachoResponse guiaDespacho;

    public static DocumentoDetailResponse from(DocumentoTributarioEntity e) {
        List<DetalleResponse> detalles = e.getDetalles() == null ? List.of()
                : e.getDetalles().stream().map(DetalleResponse::from).toList();
        List<ReferenciaResponse> referencias = e.getReferencias() == null ? List.of()
                : e.getReferencias().stream().map(ReferenciaResponse::from).toList();
        return new DocumentoDetailResponse(
                DocumentoResponse.from(e),
                detalles,
                referencias,
                e.getGuiaDespacho() != null ? GuiaDespachoResponse.from(e.getGuiaDespacho()) : null
        );
    }
}
