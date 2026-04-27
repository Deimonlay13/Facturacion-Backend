package com.gdl.facturacion_backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "detalle_documento")
@Getter
@Setter
public class DetalleDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_documento", nullable = false)
    private DocumentoTributarioEntity documento;

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = true)
    private ProductoEntity producto;

    @Column(name = "descripcion_item")
    private String descripcionItem;

    private BigDecimal cantidad;

    @Column(name = "unidad_medida")
    private String unidadMedida;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    private BigDecimal subtotal;
}