package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipo_documento")
@Getter
@Setter
public class TipoDocumentoEntity extends BaseGlobalEntity {

    @Column(name = "codigo_sii", unique = true, nullable = false)
    private Integer codigoSii;

    private String descripcion;

}