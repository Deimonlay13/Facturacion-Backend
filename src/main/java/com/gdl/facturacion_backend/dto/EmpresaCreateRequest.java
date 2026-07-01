package com.gdl.facturacion_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaCreateRequest {

    @NotBlank(message = "El RUT de la empresa es obligatorio")
    private String rutEmpresa;

    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    private String nombreFantasia;
    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String pais;
    private String telefono;
    private String sitioWeb;

    @Email(message = "El email principal no tiene un formato válido")
    private String emailPrincipal;

    @Email(message = "El email de contabilidad no tiene un formato válido")
    private String emailContabilidad;

    private String rutRepresentante;
    private String nombreRepresentante;
    private String telefonoRepresentante;
}
