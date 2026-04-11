package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "referencia_documento")
@Getter
@Setter
public class ReferenciaDocumentoEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_documento", nullable = false)
    private DocumentoTributarioEntity documento;

    @ManyToOne
    @JoinColumn(name = "id_documento_referenciado")
    private DocumentoTributarioEntity documentoReferenciado;

    @Column(name = "tipo_referencia")
    private String tipoReferencia;

    private String motivo;
}