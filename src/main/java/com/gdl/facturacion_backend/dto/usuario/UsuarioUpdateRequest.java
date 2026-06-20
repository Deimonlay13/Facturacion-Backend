package com.gdl.facturacion_backend.dto.usuario;

import lombok.Data;

@Data
public class UsuarioUpdateRequest {

    /** Opcional. Si viene, debe ser único. */
    private String username;

    /** Opcional. Si viene, se vuelve a codificar; si es null/vacío, se mantiene la actual. */
    private String password;
}
