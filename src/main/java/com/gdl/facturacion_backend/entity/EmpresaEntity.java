package com.gdl.facturacion_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empresas")
@Getter
@Setter
public class EmpresaEntity {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rut_empresa", unique = true, nullable = false)
    private String rutEmpresa;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(name = "nombre_fantasia")
    private String nombreFantasia;

    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String pais;
    private String telefono;

    @Column(name = "sitio_web")
    private String sitioWeb;

    @Column(name = "email_principal")
    private String emailPrincipal;

    @Column(name = "email_contabilidad")
    private String emailContabilidad;

    private Boolean activo;

    @Column(name = "rut_representante")
    private String rutRepresentante;

    @Column(name = "nombre_representante")
    private String nombreRepresentante;

    @Column(name = "telefono_representante")
    private String telefonoRepresentante;
}

