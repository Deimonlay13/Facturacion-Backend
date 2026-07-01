package com.gdl.facturacion_backend.dto.folio;

import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import lombok.Value;

import java.time.LocalDate;

@Value
public class CafResponse {
    Long id;
    Integer codigoTipoDocumento;
    String tipoDocumento;
    Integer rangoDesde;
    Integer rangoHasta;
    LocalDate fechaAutorizacion;
    LocalDate fechaVencimiento;
    String estado;
    long foliosGenerados;
    long foliosDisponibles;

    public static CafResponse from(CafEntity e, long foliosGenerados, long foliosDisponibles) {
        TipoDocumentoEntity tipo = e.getTipoDocumento();
        return new CafResponse(
                e.getId(),
                tipo != null ? tipo.getCodigoSii() : null,
                tipo != null ? tipo.getDescripcion() : null,
                e.getRangoDesde(),
                e.getRangoHasta(),
                e.getFechaAutorizacion(),
                e.getFechaVencimiento(),
                e.getEstado() != null ? e.getEstado().name() : null,
                foliosGenerados,
                foliosDisponibles
        );
    }
}
