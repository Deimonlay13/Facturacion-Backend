package com.gdl.facturacion_backend.entity;

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
@Table(name = "clientes")
@Getter
@Setter
public class ClienteEntity {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private EmpresaEntity empresa;

    private String rut;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "nombre_fantasia")
    private String nombreFantasia;

    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String pais;
    private String telefono;
    private String email;
    private Boolean activo;
}
