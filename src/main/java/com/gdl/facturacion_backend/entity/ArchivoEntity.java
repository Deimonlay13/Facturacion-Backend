package com.gdl.facturacion_backend.entity;

import com.gdl.facturacion_backend.enums.TipoArchivo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "archivos")
@Getter
@Setter
public class ArchivoEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_documento")
    private DocumentoTributarioEntity documento;

    @Enumerated(EnumType.STRING)
    private TipoArchivo tipo;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    private String ruta;
}