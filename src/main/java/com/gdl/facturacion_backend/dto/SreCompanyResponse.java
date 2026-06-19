package com.gdl.facturacion_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SreCompanyResponse {

    @JsonProperty("rut")
    private String rut;

    @JsonProperty("razon_social")
    private String razonSocial;

    @JsonProperty("nombre_fantasia")
    private String nombreFantasia;

    @JsonProperty("giro")
    private String giro;

    @JsonProperty("direccion")
    private String direccion;

    @JsonProperty("ciudad")
    private String ciudad;

    @JsonProperty("comuna")
    private String comuna;

    @JsonProperty("region")
    private String region;

    @JsonProperty("codigo_region")
    private String codigoRegion;

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("email")
    private String email;

    @JsonProperty("activo")
    private Boolean activo;

    /** Present when the API cannot find the RUT or returns an error. */
    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    public boolean hasError() {
        return error != null || message != null;
    }
}
