package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clientes", indexes = {
        @Index(name = "idx_cliente_empresa_rut", columnList = "id_empresa,rut")
})
@Getter
@Setter
public class ClienteEntity extends BaseModelEntity {

    @Column(nullable = false)
    private String rut;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "nombre_fantasia")
    private String nombreFantasia;

    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String region;
    private String pais;

    private String telefono;
    private String email;

    @Column(nullable = false)
    private Boolean activo = true;
}