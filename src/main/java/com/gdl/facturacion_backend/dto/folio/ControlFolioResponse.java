package com.gdl.facturacion_backend.dto.folio;

import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import lombok.Value;

@Value
public class ControlFolioResponse {
    Long id;
    Long cafActivoId;
    Integer codigoTipoDocumento;
    String tipoDocumento;
    Integer rangoDesde;
    Integer rangoHasta;
    Integer ultimoFolioUtilizado;
    String estadoCaf;

    public static ControlFolioResponse from(ControlFolioEntity e) {
        TipoDocumentoEntity tipo = e.getTipoDocumento();
        CafEntity caf = e.getCafActivo();
        return new ControlFolioResponse(
                e.getId(),
                caf != null ? caf.getId() : null,
                tipo != null ? tipo.getCodigoSii() : null,
                tipo != null ? tipo.getDescripcion() : null,
                caf != null ? caf.getRangoDesde() : null,
                caf != null ? caf.getRangoHasta() : null,
                e.getUltimoFolioUtilizado(),
                caf != null && caf.getEstado() != null ? caf.getEstado().name() : null
        );
    }
}
