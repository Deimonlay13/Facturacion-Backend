package com.gdl.facturacion_backend.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarRolRequest {

    @NotBlank(message = "El rol es obligatorio")
    private String rol;
}
