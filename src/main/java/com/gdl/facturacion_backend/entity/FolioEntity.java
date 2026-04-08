package com.gdl.facturacion_backend.entity;



import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "folios")
@Getter
@Setter
public class FolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private EmpresaEntity empresa;

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private TipoDocumentoEntity tipoDocumento;

    private Integer numero;
    private String estado;
}
