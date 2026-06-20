package com.gdl.facturacion_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "control_folios")
@Getter
@Setter
public class ControlFolioEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private TipoDocumentoEntity tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_caf_activo")
    private CafEntity cafActivo;

    @Column(name = "ultimo_folio_utilizado")
    private Integer ultimoFolioUtilizado;

    @Column(name = "ultima_fecha_emision")
    private LocalDate ultimaFechaEmision;
}
