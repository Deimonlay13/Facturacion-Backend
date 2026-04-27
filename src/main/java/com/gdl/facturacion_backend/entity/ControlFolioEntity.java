package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "control_folios")
@Getter
@Setter
public class ControlFolioEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private TipoDocumentoEntity tipoDocumento;

    @Column(name = "rango_desde")
    private Integer rangoDesde;

    @Column(name = "rango_hasta")
    private Integer rangoHasta;

    @Column(name = "ultimo_folio_utilizado")
    private Integer ultimoFolioUtilizado;

    @Column(columnDefinition = "TEXT")
    private String cafXml;
}