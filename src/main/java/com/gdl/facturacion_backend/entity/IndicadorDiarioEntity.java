package com.gdl.facturacion_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
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
    private BigDecimal valorDolar;

    @Column(name = "valor_uf")
    private BigDecimal valorUf;

    @Column(name = "valor_euro")
    private BigDecimal valorEuro;

    @Column(name = "valor_utm")
    private BigDecimal valorUtm;
}

