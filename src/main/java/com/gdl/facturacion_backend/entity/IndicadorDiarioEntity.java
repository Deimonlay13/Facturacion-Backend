package com.gdl.facturacion_backend.entity;


import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "indicadores_diarios")
@Getter
@Setter
public class IndicadorDiarioEntity {

    @Id
    private LocalDate fecha;

    @Column(name = "valor_dolar")
    private Double valorDolar;

    @Column(name = "valor_uf")
    private Double valorUf;

    @Column(name = "valor_euro")
    private Double valorEuro;

    @Column(name = "valor_utm")
    private Double valorUtm;
}

