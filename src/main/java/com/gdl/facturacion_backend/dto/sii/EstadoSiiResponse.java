package com.gdl.facturacion_backend.dto.sii;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EnvioSiiEntity;
import com.gdl.facturacion_backend.entity.HistorialEstadoSiiEntity;
import lombok.Value;

import java.util.List;

@Value
public class EstadoSiiResponse {
    Long documentoId;
    Integer folio;
    String estadoSii;
    Boolean xmlFirmado;
    String trackId;
    EnvioSiiResponse envio;
    List<HistorialSiiResponse> historial;

    public static EstadoSiiResponse from(DocumentoTributarioEntity doc,
                                         EnvioSiiEntity envio,
                                         List<HistorialEstadoSiiEntity> historial) {
        return new EstadoSiiResponse(
                doc.getId(),
                doc.getFolio(),
                doc.getEstadoSii() != null ? doc.getEstadoSii().name() : null,
                doc.getXmlFirmado(),
                envio != null ? envio.getTrackId() : null,
                EnvioSiiResponse.from(envio),
                historial.stream().map(HistorialSiiResponse::from).toList());
    }
}
