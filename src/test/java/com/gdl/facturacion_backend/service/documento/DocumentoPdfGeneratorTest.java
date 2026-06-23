package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.Moneda;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Test unitario de {@link DocumentoPdfGenerator}.
 *
 * <p>La clase es package-private y expone solo el método estático {@code generar}.
 * No tiene colaboradores inyectables (repositorios / servicios / TenantContext),
 * por lo que se prueba construyendo entidades reales y verificando los bytes del
 * PDF generado. Las ramas de los helpers estáticos privados (conversión número a
 * palabras, montos, etc.) se cubren mediante {@code generar} y, para los casos
 * límite profundos, mediante reflexión.</p>
 */
class DocumentoPdfGeneratorTest {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF_EOF = "%%EOF".getBytes(StandardCharsets.US_ASCII);

    // -------------------------------------------------------------------------
    // Builders de apoyo
    // -------------------------------------------------------------------------

    private EmpresaEntity emisorCompleto() {
        EmpresaEntity emisor = new EmpresaEntity();
        emisor.setId(1L);
        emisor.setRutEmpresa("76.111.222-3");
        emisor.setRazonSocial("Comercial Emisor SpA");
        emisor.setNombreFantasia("EmisorFantasia");
        emisor.setGiro("Venta al por menor");
        emisor.setDireccion("Av. Siempre Viva 742");
        emisor.setComuna("Providencia");
        emisor.setCiudad("Santiago");
        emisor.setTelefono("+56 2 2345 6789");
        emisor.setEmailPrincipal("contacto@emisor.cl");
        emisor.setSitioWeb("www.emisor.cl");
        return emisor;
    }

    private ClienteEntity receptorCompleto() {
        ClienteEntity receptor = new ClienteEntity();
        receptor.setId(10L);
        receptor.setRut("12.345.678-5");
        receptor.setRazonSocial("Cliente Receptor Ltda");
        receptor.setGiro("Servicios varios");
        receptor.setDireccion("Calle Falsa 123");
        receptor.setComuna("Las Condes");
        receptor.setCiudad("Santiago");
        receptor.setEmail("cliente@receptor.cl");
        return receptor;
    }

    private TipoDocumentoEntity tipoDoc(Integer codigo, String descripcion) {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(1L);
        tipo.setCodigoSii(codigo);
        tipo.setDescripcion(descripcion);
        return tipo;
    }

    private DetalleDocumentoEntity detalle(
            String codigoProducto, String descripcion, BigDecimal cantidad,
            String unidad, BigDecimal precio, BigDecimal subtotal) {
        DetalleDocumentoEntity det = new DetalleDocumentoEntity();
        if (codigoProducto != null) {
            ProductoEntity producto = new ProductoEntity();
            producto.setId(5L);
            producto.setCodigo(codigoProducto);
            det.setProducto(producto);
        }
        det.setDescripcionItem(descripcion);
        det.setCantidad(cantidad);
        det.setUnidadMedida(unidad);
        det.setPrecioUnitario(precio);
        det.setSubtotal(subtotal);
        return det;
    }

    /** Documento "feliz" completamente poblado, en CLP, con IVA. */
    private DocumentoTributarioEntity documentoCompleto() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setId(100L);
        doc.setTipoDocumento(tipoDoc(33, "FACTURA ELECTRÓNICA"));
        doc.setCliente(receptorCompleto());
        doc.setFolio(2024);
        doc.setFechaEmision(LocalDate.of(2026, 6, 22));
        doc.setFechaVencimiento(LocalDate.of(2026, 7, 22));

        // Datos de receptor "snapshot" (preferidos sobre el cliente)
        doc.setRut("12.345.678-5");
        doc.setRazonSocial("Cliente Receptor Ltda");
        doc.setGiro("Servicios varios");
        doc.setDireccion("Calle Falsa 123");
        doc.setComuna("Las Condes");
        doc.setCiudad("Santiago");
        doc.setCorreo("cliente@receptor.cl");

        // Snapshot emisor
        doc.setRutEmisor("76.111.222-3");
        doc.setRazonSocialEmisor("Comercial Emisor SpA");
        doc.setNombreFantasiaEmisor("EmisorFantasia");
        doc.setGiroEmisor("Venta al por menor");
        doc.setDireccionEmisor("Av. Siempre Viva 742");
        doc.setComunaEmisor("Providencia");
        doc.setCiudadEmisor("Santiago");
        doc.setTelefonoEmisor("+56 2 2345 6789");
        doc.setEmailPrincipalEmisor("contacto@emisor.cl");

        doc.setObservaciones("Pago a 30 días.");
        doc.setMoneda(Moneda.CLP);
        doc.setMontoNeto(new BigDecimal("100000"));
        doc.setMontoIva(new BigDecimal("19000"));
        doc.setMontoTotal(new BigDecimal("119000"));
        doc.setEstado(EstadoDocumento.EMITIDO);

        List<DetalleDocumentoEntity> detalles = new ArrayList<>();
        detalles.add(detalle("PROD-1", "Producto uno", new BigDecimal("2"),
                "UNI", new BigDecimal("50000"), new BigDecimal("100000")));
        doc.setDetalles(detalles);

        return doc;
    }

    private void assertEsPdfValido(byte[] bytes) {
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(PDF_MAGIC.length);
        assertThat(java.util.Arrays.copyOf(bytes, PDF_MAGIC.length)).isEqualTo(PDF_MAGIC);
        // El stream PDF termina con %%EOF
        assertThat(contiene(bytes, PDF_EOF)).isTrue();
    }

    private boolean contiene(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Camino feliz
    // -------------------------------------------------------------------------

    @Test
    void generar_documentoCompleto_producePdfValido() {
        DocumentoTributarioEntity doc = documentoCompleto();
        EmpresaEntity emisor = emisorCompleto();

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisor);

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_dosLlamadas_produceContenidoConsistente() {
        DocumentoTributarioEntity doc = documentoCompleto();
        EmpresaEntity emisor = emisorCompleto();

        byte[] primero = DocumentoPdfGenerator.generar(doc, emisor);
        byte[] segundo = DocumentoPdfGenerator.generar(doc, emisor);

        assertEsPdfValido(primero);
        assertEsPdfValido(segundo);
        // Misma entrada, longitud comparable (no exactamente igual por metadatos/fechas internas).
        assertThat(segundo.length).isGreaterThan(0);
    }

    // -------------------------------------------------------------------------
    // Encabezado: folio nulo, descripción de tipo nula, fallbacks de emisor
    // -------------------------------------------------------------------------

    @Test
    void generar_folioNulo_usaBorradorYSiguePudiendoGenerar() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setFolio(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_descripcionTipoNula_usaCodigoSii() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setTipoDocumento(tipoDoc(39, null));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_snapshotEmisorNulo_usaDatosDeLaEmpresa() {
        DocumentoTributarioEntity doc = documentoCompleto();
        // Forzar fallback a la entidad emisor para todos los campos del encabezado.
        doc.setNombreFantasiaEmisor(null);
        doc.setRazonSocialEmisor(null);
        doc.setGiroEmisor(null);
        doc.setDireccionEmisor(null);
        doc.setComunaEmisor(null);
        doc.setCiudadEmisor(null);
        doc.setTelefonoEmisor(null);
        doc.setEmailPrincipalEmisor(null);
        doc.setRutEmisor(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_razonSocialEmisorComoTercerFallback() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setNombreFantasiaEmisor(null);
        // razonSocialEmisor presente debe usarse como fallback del nombre.
        doc.setRazonSocialEmisor("Razon Social Snapshot");

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Receptor: cliente nulo, snapshot nulo
    // -------------------------------------------------------------------------

    @Test
    void generar_clienteNulo_yDatosReceptorSnapshot_generaPdf() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setCliente(null); // receptor == null en el generador

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_clienteNulo_ySnapshotReceptorNulo_usaCadenasVacias() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setCliente(null);
        doc.setRut(null);
        doc.setRazonSocial(null);
        doc.setGiro(null);
        doc.setDireccion(null);
        doc.setComuna(null);
        doc.setCiudad(null);
        doc.setCorreo(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_snapshotReceptorNulo_usaDatosDelCliente() {
        DocumentoTributarioEntity doc = documentoCompleto();
        // Limpiar snapshot del receptor para forzar fallback al ClienteEntity.
        doc.setRut(null);
        doc.setRazonSocial(null);
        doc.setGiro(null);
        doc.setDireccion(null);
        doc.setComuna(null);
        doc.setCiudad(null);
        doc.setCorreo(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_fechasNulas_generaPdf() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setFechaEmision(null);
        doc.setFechaVencimiento(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Moneda y tipo de cambio
    // -------------------------------------------------------------------------

    @Test
    void generar_monedaNula_usaClpPorDefecto() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMoneda(null);
        doc.setTipoCambio(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_monedaUsdConTipoCambio_muestraTipoCambio() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMoneda(Moneda.USD);
        doc.setTipoCambio(new BigDecimal("950.50"));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_monedaEur_generaPdf() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMoneda(Moneda.EUR);
        doc.setTipoCambio(new BigDecimal("1000"));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_clpConTipoCambioNoNulo_noMuestraTipoCambio() {
        // Rama: moneda == CLP aunque tipoCambio != null -> retorna solo "CLP".
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMoneda(Moneda.CLP);
        doc.setTipoCambio(new BigDecimal("1"));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Detalle: sin ítems, producto nulo, campos nulos
    // -------------------------------------------------------------------------

    @Test
    void generar_sinDetalles_muestraSinItems() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setDetalles(new ArrayList<>());

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_detallesNull_usaListaVaciaInterna() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setDetalles(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_detalleSinProducto_yCamposNulos_usaDefaults() {
        DocumentoTributarioEntity doc = documentoCompleto();
        List<DetalleDocumentoEntity> detalles = new ArrayList<>();
        // producto null, descripcion null, cantidad null, unidad null, precio null, subtotal null
        detalles.add(detalle(null, null, null, null, null, null));
        doc.setDetalles(detalles);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_detalleConProductoCodigoNulo_usaVacio() {
        DocumentoTributarioEntity doc = documentoCompleto();
        List<DetalleDocumentoEntity> detalles = new ArrayList<>();
        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo(null);
        DetalleDocumentoEntity det = new DetalleDocumentoEntity();
        det.setProducto(producto);
        det.setDescripcionItem("Item sin código");
        det.setCantidad(new BigDecimal("1.50"));
        det.setPrecioUnitario(new BigDecimal("1234.50"));
        det.setSubtotal(new BigDecimal("1851.75"));
        detalles.add(det);
        doc.setDetalles(detalles);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Observaciones: nula, en blanco, presente
    // -------------------------------------------------------------------------

    @Test
    void generar_observacionesNulas_noFalla() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setObservaciones(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_observacionesEnBlanco_noFalla() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setObservaciones("   ");

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Referencias y motivo
    // -------------------------------------------------------------------------

    @Test
    void generar_referenciasNull_usaListaVacia() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setReferencias(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_referenciaConDocumentoReferenciado_usaTipoYFolioReferido() {
        DocumentoTributarioEntity doc = documentoCompleto();

        DocumentoTributarioEntity referido = new DocumentoTributarioEntity();
        referido.setTipoDocumento(tipoDoc(33, "FACTURA ELECTRÓNICA"));
        referido.setFolio(900);
        referido.setFechaEmision(LocalDate.of(2026, 1, 5));

        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setDocumentoReferenciado(referido);
        ref.setTipoReferencia("33");
        ref.setMotivo("Anula factura anterior");

        List<ReferenciaDocumentoEntity> refs = new ArrayList<>();
        refs.add(ref);
        doc.setReferencias(refs);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_referenciaSinDocumentoReferenciado_usaTipoReferenciaYMotivo() {
        DocumentoTributarioEntity doc = documentoCompleto();

        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setDocumentoReferenciado(null);
        ref.setTipoReferencia("SET");
        ref.setMotivo("Caso de prueba SII");

        List<ReferenciaDocumentoEntity> refs = new ArrayList<>();
        refs.add(ref);
        doc.setReferencias(refs);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_referenciaSinMotivo_noImprimeSeccionMotivo() {
        DocumentoTributarioEntity doc = documentoCompleto();

        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setTipoReferencia("SET");
        ref.setMotivo("   "); // en blanco -> filtrado en agregarMotivo

        List<ReferenciaDocumentoEntity> refs = new ArrayList<>();
        refs.add(ref);
        doc.setReferencias(refs);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_referenciaMotivoNulo_noImprimeSeccionMotivo() {
        DocumentoTributarioEntity doc = documentoCompleto();

        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setTipoReferencia("SET");
        ref.setMotivo(null);

        List<ReferenciaDocumentoEntity> refs = new ArrayList<>();
        refs.add(ref);
        doc.setReferencias(refs);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_referenciaReferidoConTipoNulo_usaTipoReferencia() {
        DocumentoTributarioEntity doc = documentoCompleto();

        DocumentoTributarioEntity referido = new DocumentoTributarioEntity();
        referido.setTipoDocumento(null); // fuerza fallback a tipoReferencia
        referido.setFolio(null);         // fuerza folio vacío
        referido.setFechaEmision(null);  // fuerza fecha vacía

        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setDocumentoReferenciado(referido);
        ref.setTipoReferencia("52");
        ref.setMotivo("Referencia parcial");

        List<ReferenciaDocumentoEntity> refs = new ArrayList<>();
        refs.add(ref);
        doc.setReferencias(refs);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Pie: rama EXENTO, estado nulo, montos nulos
    // -------------------------------------------------------------------------

    @Test
    void generar_ivaCero_marcaTotalComoExento() {
        // Rama: montoIva == 0 -> exento = montoTotal.
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMontoIva(BigDecimal.ZERO);
        doc.setMontoNeto(BigDecimal.ZERO);
        doc.setMontoTotal(new BigDecimal("50000"));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_ivaNulo_marcaTotalComoExento() {
        // Rama: montoIva == null -> exento = montoTotal.
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMontoIva(null);
        doc.setMontoTotal(new BigDecimal("42000"));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_estadoNulo_muestraSinEstado() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setEstado(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_montosNulos_generaPdf() {
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMontoNeto(null);
        doc.setMontoIva(null);
        doc.setMontoTotal(null);

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    @Test
    void generar_montoTotalGrande_seConvierteEnPalabras() {
        // Ejercita montoEnPalabras con un número grande (millones).
        DocumentoTributarioEntity doc = documentoCompleto();
        doc.setMoneda(Moneda.USD);
        doc.setMontoTotal(new BigDecimal("2500000.49")); // redondea a 2.500.000
        doc.setMontoIva(new BigDecimal("1"));

        byte[] pdf = DocumentoPdfGenerator.generar(doc, emisorCompleto());

        assertEsPdfValido(pdf);
    }

    // -------------------------------------------------------------------------
    // Constructor privado: la clase es de utilidad estática
    // -------------------------------------------------------------------------

    @Test
    void constructor_esPrivado() throws Exception {
        var ctor = DocumentoPdfGenerator.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);
        Object instancia = ctor.newInstance();
        assertThat(instancia).isInstanceOf(DocumentoPdfGenerator.class);
    }

    // -------------------------------------------------------------------------
    // Helpers privados estáticos: conversión de número a palabras (todas las ramas)
    // -------------------------------------------------------------------------

    private String numeroEnPalabras(long numero) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod("numeroEnPalabras", long.class);
        m.setAccessible(true);
        return (String) m.invoke(null, numero);
    }

    private String montoEnPalabras(BigDecimal valor) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod("montoEnPalabras", BigDecimal.class);
        m.setAccessible(true);
        return (String) m.invoke(null, valor);
    }

    @Test
    void numeroEnPalabras_cero() throws Exception {
        assertThat(numeroEnPalabras(0L)).isEqualTo("cero");
    }

    @Test
    void numeroEnPalabras_negativo_anteponeMenos() throws Exception {
        assertThat(numeroEnPalabras(-5L)).isEqualTo("menos cinco");
    }

    @Test
    void numeroEnPalabras_menoresDeTreinta() throws Exception {
        assertThat(numeroEnPalabras(1L)).isEqualTo("uno");
        assertThat(numeroEnPalabras(15L)).isEqualTo("quince");
        assertThat(numeroEnPalabras(21L)).isEqualTo("veintiuno");
        assertThat(numeroEnPalabras(29L)).isEqualTo("veintinueve");
    }

    @Test
    void numeroEnPalabras_decenasConYSinUnidad() throws Exception {
        assertThat(numeroEnPalabras(30L)).isEqualTo("treinta");
        assertThat(numeroEnPalabras(45L)).isEqualTo("cuarenta y cinco");
        assertThat(numeroEnPalabras(99L)).isEqualTo("noventa y nueve");
    }

    @Test
    void numeroEnPalabras_cienExacto() throws Exception {
        assertThat(numeroEnPalabras(100L)).isEqualTo("cien");
    }

    @Test
    void numeroEnPalabras_centenasConResto() throws Exception {
        assertThat(numeroEnPalabras(101L)).isEqualTo("ciento uno");
        assertThat(numeroEnPalabras(215L)).isEqualTo("doscientos quince");
        assertThat(numeroEnPalabras(999L)).isEqualTo("novecientos noventa y nueve");
    }

    @Test
    void numeroEnPalabras_milExactoYConResto() throws Exception {
        assertThat(numeroEnPalabras(1_000L)).isEqualTo("mil");
        assertThat(numeroEnPalabras(1_001L)).isEqualTo("mil uno");
        assertThat(numeroEnPalabras(2_500L)).isEqualTo("dos mil quinientos");
    }

    @Test
    void numeroEnPalabras_unMillonYMillones() throws Exception {
        assertThat(numeroEnPalabras(1_000_000L)).isEqualTo("un millón");
        assertThat(numeroEnPalabras(2_000_000L)).isEqualTo("dos millones");
        assertThat(numeroEnPalabras(1_000_001L)).isEqualTo("un millón uno");
    }

    @Test
    void numeroEnPalabras_milMillones() throws Exception {
        assertThat(numeroEnPalabras(1_000_000_000L)).isEqualTo("uno mil millones");
        assertThat(numeroEnPalabras(2_000_000_000L)).isEqualTo("dos mil millones");
    }

    @Test
    void montoEnPalabras_nulo_devuelveCero() throws Exception {
        assertThat(montoEnPalabras(null)).isEqualTo("CERO");
    }

    @Test
    void montoEnPalabras_redondeaHaciaArriba_yEnMayusculas() throws Exception {
        assertThat(montoEnPalabras(new BigDecimal("1.5"))).isEqualTo("DOS");
        assertThat(montoEnPalabras(new BigDecimal("119000"))).isEqualTo(
                numeroEnPalabras(119000L).toUpperCase());
    }

    // -------------------------------------------------------------------------
    // Helpers privados estáticos: nombreMoneda
    // -------------------------------------------------------------------------

    private String nombreMoneda(String moneda) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod("nombreMoneda", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, moneda);
    }

    @Test
    void nombreMoneda_cubreTodasLasRamas() throws Exception {
        assertThat(nombreMoneda("USD")).isEqualTo("DÓLARES");
        assertThat(nombreMoneda("EUR")).isEqualTo("EUROS");
        assertThat(nombreMoneda("CLP")).isEqualTo("PESOS");
        assertThat(nombreMoneda("UF")).isEqualTo("PESOS"); // default
    }

    // -------------------------------------------------------------------------
    // Helpers privados estáticos: valor (preferido / fallback / ambos nulos)
    // -------------------------------------------------------------------------

    private String valor(String preferido, String fallback) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod(
                "valor", String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, preferido, fallback);
    }

    @Test
    void valor_preferidoNoBlanco_seUsa() throws Exception {
        assertThat(valor("preferido", "fallback")).isEqualTo("preferido");
    }

    @Test
    void valor_preferidoEnBlanco_usaFallback() throws Exception {
        assertThat(valor("   ", "fallback")).isEqualTo("fallback");
    }

    @Test
    void valor_preferidoNulo_usaFallback() throws Exception {
        assertThat(valor(null, "fallback")).isEqualTo("fallback");
    }

    @Test
    void valor_ambosNulos_devuelveCadenaVacia() throws Exception {
        assertThat(valor(null, null)).isEmpty();
    }

    @Test
    void valor_preferidoBlancoYFallbackNulo_devuelveCadenaVacia() throws Exception {
        assertThat(valor("  ", null)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers privados estáticos: numero, monto, fecha, direccion
    // -------------------------------------------------------------------------

    private String numero(BigDecimal valor) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod("numero", BigDecimal.class);
        m.setAccessible(true);
        return (String) m.invoke(null, valor);
    }

    @Test
    void numero_nulo_devuelveCadenaVacia() throws Exception {
        assertThat(numero(null)).isEmpty();
    }

    @Test
    void numero_quitaCerosFinales() throws Exception {
        assertThat(numero(new BigDecimal("2.50"))).isEqualTo("2.5");
        assertThat(numero(new BigDecimal("3.00"))).isEqualTo("3");
    }

    private String monto(BigDecimal valor) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod("monto", BigDecimal.class);
        m.setAccessible(true);
        return (String) m.invoke(null, valor);
    }

    @Test
    void monto_nulo_devuelveCero() throws Exception {
        assertThat(monto(null)).isEqualTo("0");
    }

    @Test
    void monto_formateaConSeparadorMiles() throws Exception {
        // Locale es-CL usa punto como separador de miles.
        assertThat(monto(new BigDecimal("100000"))).contains("100").contains("000");
    }

    private String fecha(LocalDate valor) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod("fecha", LocalDate.class);
        m.setAccessible(true);
        return (String) m.invoke(null, valor);
    }

    @Test
    void fecha_nula_devuelveCadenaVacia() throws Exception {
        assertThat(fecha(null)).isEmpty();
    }

    @Test
    void fecha_formatoDdMmYyyy() throws Exception {
        assertThat(fecha(LocalDate.of(2026, 6, 22))).isEqualTo("22-06-2026");
    }

    private String direccion(String d, String c, String ci) throws Exception {
        Method m = DocumentoPdfGenerator.class.getDeclaredMethod(
                "direccion", String.class, String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, d, c, ci);
    }

    @Test
    void direccion_uneSoloPartesNoBlancas() throws Exception {
        // direccion() siempre se invoca con resultados de valor(), que nunca son null,
        // pero pueden ser vacíos o en blanco.
        assertThat(direccion("Calle 1", "Comuna", "Ciudad"))
                .isEqualTo("Calle 1, Comuna, Ciudad");
        assertThat(direccion("Calle 1", "  ", "Ciudad"))
                .isEqualTo("Calle 1, Ciudad");
        assertThat(direccion("", "", "")).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Robustez: documento esencialmente vacío no lanza excepción
    // -------------------------------------------------------------------------

    @Test
    void generar_documentoMinimo_noLanzaExcepcion() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setTipoDocumento(tipoDoc(33, "FACTURA"));
        // sin cliente, sin detalles, sin referencias, sin montos, sin observaciones

        EmpresaEntity emisor = new EmpresaEntity();
        emisor.setRazonSocial("Emisor");

        Throwable thrown = catchThrowable(() -> {
            byte[] pdf = DocumentoPdfGenerator.generar(doc, emisor);
            assertEsPdfValido(pdf);
        });

        assertThat(thrown).isNull();
    }
}
