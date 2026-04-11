package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "folios")
@Getter
@Setter
public class FolioEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoDocumentoEntity tipoDocumento;

    private Integer numero;

    private String estado;
}