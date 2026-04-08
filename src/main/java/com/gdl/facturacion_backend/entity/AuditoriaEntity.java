package com.gdl.facturacion_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
    

@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tabla;
    private String accion;

    @Column(name = "usuario_id")
    private Long usuarioId;

    private LocalDateTime fecha;

    @Column(columnDefinition = "TEXT")
    private String detalle;
}
