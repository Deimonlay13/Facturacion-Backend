package com.gdl.facturacion_backend.dto.documento.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de previsualizar un TXT: muestra lo parseado, si el cliente fue
 * encontrado y los montos calculados, sin persistir nada.
 */
public record ImportTxtPreviewResponse(
        Integer codigoTipoDocumento,
        Long clienteId,
        String clienteRazonSocial,
        boolean clienteEncontrado,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String condicionPago,
        String moneda,
        BigDecimal tipoCambio,
        String observaciones,
        List<DetallePreview> detalles,
        BigDecimal montoNeto,
        BigDecimal montoIva,
        BigDecimal montoTotal,
        List<String> advertencias
) {

    public record DetallePreview(
            Integer numero,
            String codigoProducto,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}
}
