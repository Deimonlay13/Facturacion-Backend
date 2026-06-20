package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.enums.TipoTraslado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GuiaDespachoRequest {

    @NotNull(message = "El tipo de traslado es obligatorio")
    private TipoTraslado tipoTraslado;

    private String patente;
    private String rutTransportista;
    private String nombreTransportista;

    @NotBlank(message = "La dirección de destino es obligatoria")
    private String direccionDestino;
}
