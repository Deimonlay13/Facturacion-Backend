package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "productos", indexes = {
        @Index(name = "idx_producto_empresa", columnList = "id_empresa"),
        @Index(name = "idx_producto_empresa_codigo", columnList = "id_empresa,codigo")
})
@Getter
@Setter
public class ProductoEntity extends BaseModelEntity {

    private String codigo;

    private String nombre;

    private String descripcion;

    @Column(name = "unidad_medida")
    private String unidadMedida;

    private Double precio;

    @Column(name = "afecta_iva")
    private Boolean afectaIva;

    private Boolean activo;
}