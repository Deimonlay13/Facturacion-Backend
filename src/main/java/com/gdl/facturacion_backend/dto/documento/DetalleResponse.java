package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class DetalleResponse {
    Long id;
    Long productoId;
    String descripcionItem;
    BigDecimal cantidad;
    String unidadMedida;
    BigDecimal precioUnitario;
    BigDecimal subtotal;

    public static DetalleResponse from(DetalleDocumentoEntity e) {
        return new DetalleResponse(
                e.getId(),
                e.getProducto() != null ? e.getProducto().getId() : null,
                e.getDescripcionItem(),
                e.getCantidad(),
                e.getUnidadMedida(),
                e.getPrecioUnitario(),
                e.getSubtotal()
        );
    }
}
