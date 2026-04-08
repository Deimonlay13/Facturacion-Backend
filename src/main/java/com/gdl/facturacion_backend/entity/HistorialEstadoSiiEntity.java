package com.gdl.facturacion_backend.entity;


import java.time.LocalDateTime;

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
@Table(name = "historial_estado_sii")
@Getter
@Setter
public class HistorialEstadoSiiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_documento")
    private DocumentoTributarioEntity documento;

    private String estado;

    private LocalDateTime fecha;

    @Column(columnDefinition = "TEXT")
    private String mensaje;
}