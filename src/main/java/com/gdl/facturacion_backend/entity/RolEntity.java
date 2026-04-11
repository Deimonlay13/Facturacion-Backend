package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class RolEntity extends BaseGlobalEntity {

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(name = "nombre_mostrar")
    private String nombreMostrar;

    private String descripcion;

    private Boolean activo;
}