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
@Table(name = "envio_sii")
@Getter
@Setter
public class EnvioSiiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private EmpresaEntity empresa;

    @Column(name = "track_id")
    private String trackId;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    private String estado;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    private Integer intentos;
}
