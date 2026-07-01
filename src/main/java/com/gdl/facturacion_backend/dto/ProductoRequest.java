package com.gdl.facturacion_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoRequest {

    private String codigo;
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private Double precio;
    private Boolean afectaIva;
    private Boolean activo;
}