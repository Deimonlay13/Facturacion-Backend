package com.gdl.facturacion_backend.entity;

import com.gdl.facturacion_backend.enums.TipoTraslado;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "guia_despacho_extra")
@Getter
@Setter
public class GuiaDespachoExtraEntity extends BaseModelEntity {

    @OneToOne
    @JoinColumn(name = "id_documento", nullable = false, unique = true)
    private DocumentoTributarioEntity documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_traslado")
    private TipoTraslado tipoTraslado;

    private String patente;

    @Column(name = "rut_transportista")
    private String rutTransportista;

    @Column(name = "nombre_transportista")
    private String nombreTransportista;

    @Column(name = "direccion_destino")
    private String direccionDestino;
}