package com.gdl.facturacion_backend.entity;

import java.time.LocalDateTime;

import com.gdl.facturacion_backend.enums.EstadoSii;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "historial_estado_sii")
@Getter
@Setter
public class HistorialEstadoSiiEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_documento", nullable = false)
    private DocumentoTributarioEntity documento;

    @Enumerated(EnumType.STRING)
    private EstadoSii estado;

    @Column(name = "fecha_estado")
    private LocalDateTime fechaEstado;

    @Column(columnDefinition = "TEXT")
    private String mensaje;
}