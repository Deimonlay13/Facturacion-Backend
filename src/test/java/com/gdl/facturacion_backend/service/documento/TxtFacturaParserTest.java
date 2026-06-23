package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.dto.documento.importacion.FacturaTxt;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test JUnit 5 puro para TxtFacturaParser. Se arman Strings de TXT
 * pipe-delimitados con secciones CLIENTE, CABECERA, OBSERVACION y DETALLE
 * y se verifican: tipo de DTE por prefijo del numero de documento, moneda
 * detectada, fechas, detalles, montos y todas las validaciones de error.
 */
class TxtFacturaParserTest {

    private TxtFacturaParser parser;

    @BeforeEach
    void setUp() {
        parser = new TxtFacturaParser();
    }

    // --------------------------------------------------------------- helpers

    /**
     * Construye un TXT completo. La descripcion del detalle (token 6) es donde el parser
     * detecta la moneda (ej. "PRODUCTO USD:8615.2"). El numero de documento (token 3 de
     * CLIENTE) determina el tipo de DTE por su prefijo (FA/FE/NC/ND).
     */
    private String txt(String numeroDoc, String descripcionDetalle) {
        return ""
                + "|**CLIENTE**|33|" + numeroDoc + "|EXTRA|Empresa Demo SpA|Av. Siempre Viva 123|Chile|REGION|Santiago|+56222222\n"
                + "|**CABECERA**|01-02-2024|03-04-2024|CONTADO|950.5\n"
                + "|**OBSERVACION**|Observacion de prueba|x|y|Juan Vendedor|OPER-001\n"
                + "|**DETALLE**|1|x|y|PROD-1|" + descripcionDetalle + "|2|100.50\n";
    }

    // --------------------------------------------------------------- tipo por prefijo

    @Test
    @DisplayName("Prefijo FA -> tipo 33 (factura afecta)")
    void tipoFacturaAfecta() {
        FacturaTxt f = parser.parse(txt("FA0001", "Item nacional"));
        assertThat(f.codigoTipoDocumento()).isEqualTo(33);
    }

    @Test
    @DisplayName("Prefijo FE -> tipo 34 (factura exenta)")
    void tipoFacturaExenta() {
        FacturaTxt f = parser.parse(txt("FE0002", "Item exento"));
        assertThat(f.codigoTipoDocumento()).isEqualTo(34);
    }

    @Test
    @DisplayName("Prefijo NC -> tipo 61 (nota de credito)")
    void tipoNotaCredito() {
        FacturaTxt f = parser.parse(txt("NC0003", "Devolucion"));
        assertThat(f.codigoTipoDocumento()).isEqualTo(61);
    }

    @Test
    @DisplayName("Prefijo ND -> tipo 56 (nota de debito)")
    void tipoNotaDebito() {
        FacturaTxt f = parser.parse(txt("ND0004", "Cargo adicional"));
        assertThat(f.codigoTipoDocumento()).isEqualTo(56);
    }

    @Test
    @DisplayName("Prefijo no reconocido -> usa el numero de tipo del archivo (token 2)")
    void tipoDesconocidoUsaNumeroArchivo() {
        // numeroDoc "XX0005" no matchea ningun prefijo -> cae al 33 del archivo
        FacturaTxt f = parser.parse(txt("XX0005", "Item"));
        assertThat(f.codigoTipoDocumento()).isEqualTo(33);
    }

    @Test
    @DisplayName("Prefijo en minuscula tambien se reconoce (case-insensitive)")
    void tipoPrefijoMinuscula() {
        FacturaTxt f = parser.parse(txt("nc9999", "Devolucion"));
        assertThat(f.codigoTipoDocumento()).isEqualTo(61);
    }

    // --------------------------------------------------------------- moneda

    @Test
    @DisplayName("Detecta USD en la descripcion del detalle")
    void monedaUsd() {
        FacturaTxt f = parser.parse(txt("FA1000", "Servicio USD:8615.2"));
        assertThat(f.moneda()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Detecta EUR en la descripcion del detalle")
    void monedaEur() {
        FacturaTxt f = parser.parse(txt("FA1001", "Servicio EUR"));
        assertThat(f.moneda()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Sin codigo de moneda en el detalle -> CLP por defecto")
    void monedaPorDefectoClp() {
        FacturaTxt f = parser.parse(txt("FA1002", "Producto nacional"));
        assertThat(f.moneda()).isEqualTo("CLP");
    }

    @Test
    @DisplayName("Moneda en minuscula se detecta y devuelve en mayuscula")
    void monedaMinusculaSeDetecta() {
        FacturaTxt f = parser.parse(txt("FA1003", "valor usd ahora"));
        assertThat(f.moneda()).isEqualTo("USD");
    }

    // --------------------------------------------------------------- cliente, fechas, cabecera

    @Test
    @DisplayName("Mapea correctamente los campos de CLIENTE")
    void mapeaCliente() {
        FacturaTxt f = parser.parse(txt("FA2000", "Item"));
        FacturaTxt.Cliente c = f.cliente();
        assertThat(c).isNotNull();
        assertThat(c.codigo()).isEqualTo("FA2000");
        assertThat(c.razonSocial()).isEqualTo("Empresa Demo SpA");
        assertThat(c.direccion()).isEqualTo("Av. Siempre Viva 123");
        assertThat(c.pais()).isEqualTo("Chile");
        assertThat(c.ciudad()).isEqualTo("Santiago");
        assertThat(c.telefono()).isEqualTo("+56222222");
    }

    @Test
    @DisplayName("Parsea fechas de CABECERA en formato dd-MM-yyyy")
    void parseaFechas() {
        FacturaTxt f = parser.parse(txt("FA2001", "Item"));
        assertThat(f.fechaEmision()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(f.fechaVencimiento()).isEqualTo(LocalDate.of(2024, 4, 3));
    }

    @Test
    @DisplayName("Parsea condicion de pago y tipo de cambio (coma decimal admitida)")
    void parseaCabeceraExtra() {
        FacturaTxt f = parser.parse(txt("FA2002", "Item"));
        assertThat(f.condicionPago()).isEqualTo("CONTADO");
        assertThat(f.tipoCambio()).isEqualByComparingTo(new BigDecimal("950.5"));
    }

    @Test
    @DisplayName("Tipo de cambio con coma como separador decimal se normaliza a punto")
    void tipoCambioConComa() {
        String contenido = ""
                + "|**CLIENTE**|33|FA3000|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "|**CABECERA**|01-02-2024|03-04-2024|CONTADO|1234,56\n"
                + "|**DETALLE**|1|x|y|PROD|Item|1|10\n";
        FacturaTxt f = parser.parse(contenido);
        assertThat(f.tipoCambio()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("Limpia la observacion de caracteres de control y espacios redundantes")
    void limpiaObservacion() {
        // U+FFFD es el caracter de reemplazo que el parser convierte a espacio.
        String contenido = ""
                + "|**CLIENTE**|33|FA4000|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "|**OBSERVACION**|Nota\tcon   espacios\uFFFDraros|x|y|Vend|OPER\n"
                + "|**DETALLE**|1|x|y|PROD|Item|1|10\n";
        FacturaTxt f = parser.parse(contenido);
        assertThat(f.observacion()).isEqualTo("Nota con espacios raros");
        assertThat(f.vendedor()).isEqualTo("Vend");
        assertThat(f.operacion()).isEqualTo("OPER");
    }

    // --------------------------------------------------------------- detalles y montos

    @Test
    @DisplayName("Mapea detalle con numero, codigo, descripcion, cantidad y precio")
    void mapeaDetalle() {
        FacturaTxt f = parser.parse(txt("FA5000", "Servicio premium"));
        assertThat(f.detalles()).hasSize(1);
        FacturaTxt.Detalle d = f.detalles().get(0);
        assertThat(d.numero()).isEqualTo(1);
        assertThat(d.codigoProducto()).isEqualTo("PROD-1");
        assertThat(d.descripcion()).isEqualTo("Servicio premium");
        assertThat(d.cantidad()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(d.precioUnitario()).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("Acumula multiples lineas de DETALLE")
    void variosDetalles() {
        String contenido = ""
                + "|**CLIENTE**|33|FA6000|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "|**DETALLE**|1|x|y|P1|Item uno|1|10\n"
                + "|**DETALLE**|2|x|y|P2|Item dos USD|3|25.5\n";
        FacturaTxt f = parser.parse(contenido);
        assertThat(f.detalles()).hasSize(2);
        assertThat(f.detalles().get(1).numero()).isEqualTo(2);
        assertThat(f.detalles().get(1).precioUnitario()).isEqualByComparingTo(new BigDecimal("25.5"));
        // moneda detectada en la segunda linea
        assertThat(f.moneda()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Ignora lineas en blanco y secciones desconocidas")
    void ignoraLineasBlancasYSeccionesDesconocidas() {
        String contenido = ""
                + "|**CLIENTE**|33|FA7000|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "\n"
                + "|**OTRA_SECCION**|dato|ignorado\n"
                + "   \n"
                + "|**DETALLE**|1|x|y|PROD|Item|1|10\n";
        FacturaTxt f = parser.parse(contenido);
        assertThat(f.codigoTipoDocumento()).isEqualTo(33);
        assertThat(f.detalles()).hasSize(1);
    }

    // --------------------------------------------------------------- errores / validaciones

    @Test
    @DisplayName("Contenido null lanza ReglaNegocioException")
    void contenidoNull() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("vac");
    }

    @Test
    @DisplayName("Contenido en blanco lanza ReglaNegocioException")
    void contenidoBlanco() {
        assertThatThrownBy(() -> parser.parse("   \n  "))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("vac");
    }

    @Test
    @DisplayName("Sin seccion CLIENTE valida lanza ReglaNegocioException")
    void sinClienteValido() {
        String contenido = "|**DETALLE**|1|x|y|PROD|Item|1|10\n";
        assertThatThrownBy(() -> parser.parse(contenido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("CLIENTE");
    }

    @Test
    @DisplayName("Sin lineas de DETALLE lanza ReglaNegocioException")
    void sinDetalles() {
        String contenido = "|**CLIENTE**|33|FA8000|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n";
        assertThatThrownBy(() -> parser.parse(contenido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("DETALLE");
    }

    @Test
    @DisplayName("Fecha de emision con formato invalido lanza ReglaNegocioException")
    void fechaInvalida() {
        String contenido = ""
                + "|**CLIENTE**|33|FA8100|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "|**CABECERA**|2024-02-01|03-04-2024|CONTADO|1\n"
                + "|**DETALLE**|1|x|y|PROD|Item|1|10\n";
        assertThatThrownBy(() -> parser.parse(contenido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Fecha");
    }

    @Test
    @DisplayName("Monto no numerico en el detalle lanza ReglaNegocioException")
    void montoInvalido() {
        String contenido = ""
                + "|**CLIENTE**|33|FA8200|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "|**DETALLE**|1|x|y|PROD|Item|abc|10\n";
        assertThatThrownBy(() -> parser.parse(contenido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Monto");
    }

    @Test
    @DisplayName("Numero de linea no entero en el detalle lanza ReglaNegocioException")
    void numeroLineaInvalido() {
        String contenido = ""
                + "|**CLIENTE**|33|FA8300|EXTRA|Cli|Dir|Chile|REGION|Stgo|+56\n"
                + "|**DETALLE**|uno|x|y|PROD|Item|1|10\n";
        assertThatThrownBy(() -> parser.parse(contenido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Valor");
    }
}
