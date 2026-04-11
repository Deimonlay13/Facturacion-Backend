package com.gdl.facturacion_backend.entity;

import java.time.LocalDateTime;

import com.gdl.facturacion_backend.enums.EstadoEnvioSii;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "envio_sii")
@Getter
@Setter
public class EnvioSiiEntity extends BaseModelEntity {

    @Column(name = "track_id", unique = true)
    private String trackId;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    private EstadoEnvioSii estado;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    private Integer intentos;
}