package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empresas")
@Getter
@Setter
public class EmpresaEntity  extends BaseGlobalEntity {

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

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "rut_representante")
    private String rutRepresentante;

    @Column(name = "nombre_representante")
    private String nombreRepresentante;

    @Column(name = "telefono_representante")
    private String telefonoRepresentante;

}