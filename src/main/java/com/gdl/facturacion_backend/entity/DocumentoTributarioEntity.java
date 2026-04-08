package com.gdl.facturacion_backend.entity;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DocumentoTributarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private EmpresaEntity empresa;

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private TipoDocumentoEntity tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private ClienteEntity cliente;

    private Integer folio;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    private String rut;

    @Column(name = "razon_social")
    private String razonSocial;

    private String giro;
    private String direccion;
    private String ciudad;
    private String comuna;
    private String pais;

    private String correo;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    private String moneda;

    @Column(name = "tipo_cambio")
    private Double tipoCambio;

    @Column(name = "monto_neto")
    private Double montoNeto;

    @Column(name = "monto_iva")
    private Double montoIva;

    @Column(name = "monto_total")
    private Double montoTotal;

    @Column(name = "estado_sii")
    private String estadoSii;

    @Column(name = "xml_firmado")
    private Boolean xmlFirmado;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleDocumentoEntity> detalles;

    @OneToMany(mappedBy = "documento")
    private List<ReferenciaDocumentoEntity> referencias;
}



