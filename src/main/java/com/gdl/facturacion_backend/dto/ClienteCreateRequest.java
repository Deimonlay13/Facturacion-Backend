package com.gdl.facturacion_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteCreateRequest {

    @NotBlank(message = "El RUT es obligatorio")
    private String rut;

    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    private String nombreFantasia;
    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String region;
    private String pais;
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;
}
