package com.gdl.facturacion_backend.entity;

import java.time.LocalDate;

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
@Table(name = "caf")
@Getter
@Setter
public class CafEntity {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private EmpresaEntity empresa;

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private TipoDocumentoEntity tipoDocumento;

    @Column(name = "rango_desde")
    private Integer rangoDesde;

    @Column(name = "rango_hasta")
    private Integer rangoHasta;

    @Column(name = "fecha_autorizacion")
    private LocalDate fechaAutorizacion;

    @Column(name = "clave_privada")
    private String clavePrivada;

    @Column(name = "clave_publica")
    private String clavePublica;

    private String estado;
}