package com.gdl.facturacion_backend.dto.sii;

import com.gdl.facturacion_backend.entity.HistorialEstadoSiiEntity;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Vista de administración del historial de estados SII: incluye el documento asociado
 * (id y folio) para poder verlo desde el panel de administración.
 */
@Value
public class HistorialSiiAdminResponse {
    Long id;
    Long documentoId;
    Integer folio;
    String estado;
    LocalDateTime fechaEstado;
    String mensaje;

    public static HistorialSiiAdminResponse from(HistorialEstadoSiiEntity h) {
        return new HistorialSiiAdminResponse(
                h.getId(),
                h.getDocumento() != null ? h.getDocumento().getId() : null,
                h.getDocumento() != null ? h.getDocumento().getFolio() : null,
                h.getEstado() != null ? h.getEstado().name() : null,
                h.getFechaEstado(),
                h.getMensaje());
    }
}
