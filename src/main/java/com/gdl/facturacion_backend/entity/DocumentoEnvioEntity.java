package com.gdl.facturacion_backend.entity;

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
@Table(name = "documento_envio")
@Getter
@Setter
public class DocumentoEnvioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_documento")
    private DocumentoTributarioEntity documento;

    @ManyToOne
    @JoinColumn(name = "id_envio")
    private EnvioSiiEntity envio;
}
