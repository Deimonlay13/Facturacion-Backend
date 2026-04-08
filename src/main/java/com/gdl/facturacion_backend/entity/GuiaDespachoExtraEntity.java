package com.gdl.facturacion_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "guia_despacho_extra")
@Getter
@Setter
public class GuiaDespachoExtraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_documento")
    private DocumentoTributarioEntity documento;

    @Column(name = "tipo_traslado")
    private String tipoTraslado;

    private String patente;

    @Column(name = "rut_transportista")
    private String rutTransportista;

    @Column(name = "nombre_transportista")
    private String nombreTransportista;

    @Column(name = "direccion_destino")
    private String direccionDestino;
}

