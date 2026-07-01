package com.gdl.facturacion_backend.dto.folio;

import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import lombok.Value;

import java.time.LocalDate;

@Value
public class FolioResumenResponse {
    Integer codigoTipoDocumento;
    String tipoDocumento;
    Long cafActivoId;
    Integer rangoDesde;
    Integer rangoHasta;
    Integer siguienteFolio;
    Integer ultimoFolioUtilizado;
    LocalDate ultimaFechaEmision;
    long foliosDisponibles;
    String estadoCaf;
    boolean listoParaEmitir;

    public static FolioResumenResponse from(ControlFolioEntity control,
                                            FolioEntity siguiente,
                                            long foliosDisponibles) {
        TipoDocumentoEntity tipo = control.getTipoDocumento();
        CafEntity caf = control.getCafActivo();
        return new FolioResumenResponse(
                tipo != null ? tipo.getCodigoSii() : null,
                tipo != null ? tipo.getDescripcion() : null,
                caf != null ? caf.getId() : null,
                caf != null ? caf.getRangoDesde() : null,
                caf != null ? caf.getRangoHasta() : null,
                siguiente != null ? siguiente.getNumero() : null,
                control.getUltimoFolioUtilizado(),
                control.getUltimaFechaEmision(),
                foliosDisponibles,
                caf != null && caf.getEstado() != null ? caf.getEstado().name() : null,
                caf != null && siguiente != null && foliosDisponibles > 0
        );
    }
}
