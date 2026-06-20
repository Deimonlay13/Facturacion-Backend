package com.gdl.facturacion_backend.entity;

import com.gdl.facturacion_backend.enums.EstadoFolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "folios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_folio_empresa_tipo_numero", columnNames = {"id_empresa", "id_tipo", "numero"})
})
@Getter
@Setter
public class FolioEntity extends BaseModelEntity {

    @ManyToOne
    @JoinColumn(name = "id_caf", nullable = false)
    private CafEntity caf;

    @ManyToOne
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoDocumentoEntity tipoDocumento;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFolio estado;
}
