package com.gdl.facturacion_backend.dto.documento;

import java.math.BigDecimal;

public record DocumentoEmisorStatsResponse(
        Long usuarioId,
        String usuario,
        Long documentosEmitidos,
        BigDecimal montoTotal
) {
}
