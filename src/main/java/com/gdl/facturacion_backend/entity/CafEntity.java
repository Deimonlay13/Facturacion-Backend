package com.gdl.facturacion_backend.entity;

import java.time.LocalDate;

import com.gdl.facturacion_backend.enums.EstadoCaf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "caf")
@Getter
@Setter
public class CafEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private TipoDocumentoEntity tipoDocumento;

    @Column(name = "rango_desde")
    private Integer rangoDesde;

    @Column(name = "rango_hasta")
    private Integer rangoHasta;

    @Column(name = "fecha_autorizacion")
    private LocalDate fechaAutorizacion;

    @Column(name = "fecha_vencimiento", nullable = true)
    private LocalDate fechaVencimiento;

    @Column(name = "caf_xml", columnDefinition = "TEXT")
    private String cafXml;

    @Enumerated(EnumType.STRING)
    private EstadoCaf estado;
}
