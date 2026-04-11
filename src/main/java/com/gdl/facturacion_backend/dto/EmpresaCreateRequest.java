package com.gdl.facturacion_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaCreateRequest {

    private String rutEmpresa;
    private String razonSocial;
    private String nombreFantasia;
    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String pais;
    private String telefono;
    private String sitioWeb;
    private String emailPrincipal;
    private String emailContabilidad;
    private String rutRepresentante;
    private String nombreRepresentante;
    private String telefonoRepresentante;
}