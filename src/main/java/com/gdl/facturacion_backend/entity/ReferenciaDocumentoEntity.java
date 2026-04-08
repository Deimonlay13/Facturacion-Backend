package com.gdl.facturacion_backend.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "referencia_documento")
@Getter
@Setter
public class ReferenciaDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_documento")
    private DocumentoTributarioEntity documento;

    @ManyToOne
    @JoinColumn(name = "id_documento_referenciado")
    private DocumentoTributarioEntity documentoReferenciado;

    @Column(name = "tipo_referencia")
    private String tipoReferencia;

    private String motivo;
}
