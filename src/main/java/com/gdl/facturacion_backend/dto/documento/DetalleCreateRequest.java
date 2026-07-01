package com.gdl.facturacion_backend.dto.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetalleCreateRequest {

    /** Producto opcional; si se indica debe pertenecer a la empresa. */
    private Long productoId;

    @NotBlank(message = "La descripción del ítem es obligatoria")
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    private String unidadMedida;

    @NotNull(message = "El precio unitario es obligatorio")
    @PositiveOrZero(message = "El precio unitario no puede ser negativo")
    private BigDecimal precioUnitario;
}
