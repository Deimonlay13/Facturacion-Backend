package com.gdl.facturacion_backend.dto.folio;

import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import lombok.Value;

@Value
public class ControlFolioResponse {
    Long id;
    Integer codigoTipoDocumento;
    String tipoDocumento;
    Integer rangoDesde;
    Integer rangoHasta;
    Integer ultimoFolioUtilizado;

    public static ControlFolioResponse from(ControlFolioEntity e) {
        TipoDocumentoEntity tipo = e.getTipoDocumento();
        return new ControlFolioResponse(
                e.getId(),
                tipo != null ? tipo.getCodigoSii() : null,
                tipo != null ? tipo.getDescripcion() : null,
                e.getRangoDesde(),
                e.getRangoHasta(),
                e.getUltimoFolioUtilizado()
        );
    }
}
