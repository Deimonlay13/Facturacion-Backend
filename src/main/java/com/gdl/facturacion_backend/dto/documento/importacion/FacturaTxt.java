package com.gdl.facturacion_backend.dto.documento.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Representación estructurada de un archivo TXT de factura (formato pipe-delimitado
 * con secciones **CLIENTE**, **CABECERA**, **OBSERVACION** y **DETALLE**).
 */
public record FacturaTxt(
        Integer codigoTipoDocumento,
        Cliente cliente,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String condicionPago,
        BigDecimal tipoCambio,
        String moneda,
        String observacion,
        String operacion,
        String vendedor,
        List<Detalle> detalles
) {

    public record Cliente(
            String codigo,
            String razonSocial,
            String direccion,
            String pais,
            String ciudad,
            String telefono
    ) {}

    public record Detalle(
            Integer numero,
            String codigoProducto,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario
    ) {}
}
