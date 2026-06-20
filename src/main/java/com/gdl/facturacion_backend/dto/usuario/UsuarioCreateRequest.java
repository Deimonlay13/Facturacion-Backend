package com.gdl.facturacion_backend.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsuarioCreateRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /** Opcional. Si no se indica, se asigna ROLE_USER. */
    private String rol;
}
