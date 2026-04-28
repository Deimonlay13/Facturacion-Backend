package com.gdl.facturacion_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.Moneda;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "documentos_tributarios")
@Getter
@Setter
public class DocumentoTributarioEntity extends BaseModelEntity {

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

    @Enumerated(EnumType.STRING)
    private Moneda moneda;

    @Column(name = "tipo_cambio")
    private BigDecimal tipoCambio;

    @Column(name = "monto_neto")
    private BigDecimal montoNeto;

    @Column(name = "monto_iva")
    private BigDecimal montoIva;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoDocumento estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sii")
    private EstadoDocumentoSii estadoSii;

    @Column(name = "xml_firmado")
    private Boolean xmlFirmado;

    @ManyToOne
    @JoinColumn(name = "id_envio")
    private EnvioSiiEntity envio;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleDocumentoEntity> detalles;

    @OneToMany(mappedBy = "documento")
    private List<ReferenciaDocumentoEntity> referencias;
}


