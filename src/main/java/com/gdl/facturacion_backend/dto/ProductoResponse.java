package com.gdl.facturacion_backend.dto;

import com.gdl.facturacion_backend.entity.ProductoEntity;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class ProductoResponse {
    Long id;
    String codigo;
    String nombre;
    String descripcion;
    String unidadMedida;
    Double precio;
    Boolean afectaIva;
    Boolean activo;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    public static ProductoResponse from(ProductoEntity p) {
        return new ProductoResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getUnidadMedida(),
                p.getPrecio(),
                p.getAfectaIva(),
                p.getActivo(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
