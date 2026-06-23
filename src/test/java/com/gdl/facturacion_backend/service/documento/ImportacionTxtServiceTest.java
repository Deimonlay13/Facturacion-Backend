package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.dto.documento.importacion.FacturaTxt;
import com.gdl.facturacion_backend.dto.documento.importacion.ImportTxtPreviewResponse;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.Moneda;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.ClienteRepository;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.service.TenantService;
import com.gdl.facturacion_backend.service.TipoDocumentoService;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacionTxtServiceTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private TxtFacturaParser parser;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TipoDocumentoService tipoDocumentoService;

    @Mock
    private DocumentoTributarioRepository documentoRepository;

    @Mock
    private CalculoMontosService calculoMontosService;

    @Mock
    private ReglaTributariaResolver reglaResolver;

    @Mock
    private TenantService tenantService;

    @Mock
    private ReglaTributaria reglaTributaria;

    @InjectMocks
    private ImportacionTxtService service;

    @BeforeEach
    void setUp() {
        // El servicio resuelve la empresa por TenantService.getEmpresaId().
        lenient().when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
    }

    // --------------------------------------------------------------- helpers de fixtures

    private FacturaTxt.Cliente cliente(String razonSocial) {
        return new FacturaTxt.Cliente("FA001", razonSocial, "Av Siempre Viva 742",
                "CHILE", "Santiago", "+56 2 1234567");
    }

    private FacturaTxt.Detalle detalle(Integer numero, String codigo, String descripcion,
                                       BigDecimal cantidad, BigDecimal precio) {
        return new FacturaTxt.Detalle(numero, codigo, descripcion, cantidad, precio);
    }

    private FacturaTxt facturaConDetalles(String moneda, List<FacturaTxt.Detalle> detalles) {
        return new FacturaTxt(
                33,
                cliente("ACME SPA"),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 15),
                "CONTADO",
                BigDecimal.ONE,
                moneda,
                "Observacion de prueba",
                "OP-1",
                "Vendedor X",
                detalles);
    }

    private ClienteEntity clienteEntity(Long id) {
        ClienteEntity entity = new ClienteEntity();
        entity.setId(id);
        entity.setRazonSocial("ACME SPA");
        return entity;
    }

    private TipoDocumentoEntity tipoDocumento(Integer codigoSii) {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(10L);
        tipo.setCodigoSii(codigoSii);
        tipo.setDescripcion("Factura Afecta");
        return tipo;
    }

    // ============================================================ previsualizar()

    @Test
    void previsualizar_clienteEncontrado_montosCalculadosConIva19() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "P001", "Producto uno", new BigDecimal("2"), new BigDecimal("100")),
                detalle(2, "P002", "Producto dos", new BigDecimal("3"), new BigDecimal("50"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(99L)));
        when(reglaResolver.resolver(33)).thenReturn(reglaTributaria);
        when(reglaTributaria.tasaIva()).thenReturn(new BigDecimal("0.19"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        assertThat(response.codigoTipoDocumento()).isEqualTo(33);
        assertThat(response.clienteId()).isEqualTo(99L);
        assertThat(response.clienteRazonSocial()).isEqualTo("ACME SPA");
        assertThat(response.clienteEncontrado()).isTrue();
        assertThat(response.moneda()).isEqualTo("CLP");
        assertThat(response.advertencias()).isEmpty();
        // neto = 2*100 + 3*50 = 350 ; iva = 350 * 0.19 = 66.5 -> 67 (HALF_UP) ; total = 417
        assertThat(response.montoNeto()).isEqualByComparingTo("350");
        assertThat(response.montoIva()).isEqualByComparingTo("67");
        assertThat(response.montoTotal()).isEqualByComparingTo("417");
        assertThat(response.detalles()).hasSize(2);
        ImportTxtPreviewResponse.DetallePreview primero = response.detalles().get(0);
        assertThat(primero.descripcion()).isEqualTo("P001 - Producto uno");
        assertThat(primero.subtotal()).isEqualByComparingTo("200");
        assertThat(response.detalles().get(1).subtotal()).isEqualByComparingTo("150");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void previsualizar_clienteNoEncontrado_agregaAdvertenciaYClienteIdNull() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("1000"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.empty());
        when(reglaResolver.resolver(33)).thenReturn(reglaTributaria);
        when(reglaTributaria.tasaIva()).thenReturn(new BigDecimal("0.19"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        assertThat(response.clienteEncontrado()).isFalse();
        assertThat(response.clienteId()).isNull();
        assertThat(response.clienteRazonSocial()).isEqualTo("ACME SPA");
        assertThat(response.advertencias())
                .anySatisfy(a -> assertThat(a).contains("No se encontró un cliente con razón social 'ACME SPA'"));
    }

    @Test
    void previsualizar_tipoNoSoportado_agregaAdvertenciaYTasaCero() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("1000"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(5L)));
        when(reglaResolver.resolver(33))
                .thenThrow(new ReglaNegocioException("Tipo de documento no soportado: 33"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        // Sin regla, la tasa de IVA cae a cero.
        assertThat(response.montoNeto()).isEqualByComparingTo("1000");
        assertThat(response.montoIva()).isEqualByComparingTo("0");
        assertThat(response.montoTotal()).isEqualByComparingTo("1000");
        assertThat(response.advertencias())
                .anySatisfy(a -> assertThat(a).contains("El tipo de documento 33 no está soportado"));
    }

    @Test
    void previsualizar_monedaExtranjera_fuerzaIvaCeroAunConRegla() {
        FacturaTxt factura = facturaConDetalles("USD", List.of(
                detalle(1, "P001", "Producto USD", new BigDecimal("2"), new BigDecimal("500"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(7L)));
        when(reglaResolver.resolver(33)).thenReturn(reglaTributaria);
        when(reglaTributaria.tasaIva()).thenReturn(new BigDecimal("0.19"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        // Moneda extranjera: exento de IVA aunque la regla tenga 19%.
        assertThat(response.montoNeto()).isEqualByComparingTo("1000");
        assertThat(response.montoIva()).isEqualByComparingTo("0");
        assertThat(response.montoTotal()).isEqualByComparingTo("1000");
        assertThat(response.advertencias()).isEmpty();
    }

    @Test
    void previsualizar_clienteNulo_marcaNoEncontradoYRazonSocialNull() {
        FacturaTxt factura = new FacturaTxt(33, null,
                LocalDate.of(2026, 1, 1), null, "CONTADO", null, "CLP", null, null, null,
                List.of(detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(reglaResolver.resolver(33)).thenReturn(reglaTributaria);
        when(reglaTributaria.tasaIva()).thenReturn(new BigDecimal("0.19"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        assertThat(response.clienteEncontrado()).isFalse();
        assertThat(response.clienteId()).isNull();
        assertThat(response.clienteRazonSocial()).isNull();
        assertThat(response.advertencias())
                .anySatisfy(a -> assertThat(a).contains("No se encontró un cliente con razón social 'null'"));
        // Con cliente nulo, no se consulta el repositorio de clientes.
        verify(clienteRepository, never())
                .findFirstByEmpresaIdAndRazonSocialIgnoreCase(anyLong(), anyString());
    }

    @Test
    void previsualizar_detalleSoloDescripcion_yCantidadNula_calculaSubtotalComoCero() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, null, "Servicio sin codigo", null, new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(reglaResolver.resolver(33)).thenReturn(reglaTributaria);
        when(reglaTributaria.tasaIva()).thenReturn(new BigDecimal("0.19"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        ImportTxtPreviewResponse.DetallePreview det = response.detalles().get(0);
        assertThat(det.descripcion()).isEqualTo("Servicio sin codigo");
        assertThat(det.subtotal()).isEqualByComparingTo("0");
        assertThat(response.montoNeto()).isEqualByComparingTo("0");
        assertThat(response.montoIva()).isEqualByComparingTo("0");
        assertThat(response.montoTotal()).isEqualByComparingTo("0");
    }

    @Test
    void previsualizar_detalleSoloCodigo_usaCodigoComoDescripcion_yPrecioNulo() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "SOLO-COD", null, new BigDecimal("5"), null)));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(reglaResolver.resolver(33)).thenReturn(reglaTributaria);
        when(reglaTributaria.tasaIva()).thenReturn(new BigDecimal("0.19"));

        ImportTxtPreviewResponse response = service.previsualizar("contenido");

        ImportTxtPreviewResponse.DetallePreview det = response.detalles().get(0);
        assertThat(det.descripcion()).isEqualTo("SOLO-COD");
        assertThat(det.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void previsualizar_parserLanza_propagaExcepcion() {
        when(parser.parse("malo")).thenThrow(new ReglaNegocioException("El archivo TXT está vacío"));

        assertThatThrownBy(() -> service.previsualizar("malo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El archivo TXT está vacío");

        verifyNoInteractions(clienteRepository, reglaResolver, documentoRepository);
    }

    // ================================================================= importar()

    @Test
    void importar_clienteEncontrado_creaDocumentoEnBorradorYGuarda() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "P001", "Producto uno", new BigDecimal("2"), new BigDecimal("100"))));
        ClienteEntity cliente = clienteEntity(99L);
        TipoDocumentoEntity tipo = tipoDocumento(33);

        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(cliente));
        when(tipoDocumentoService.findByCodigoSii(33)).thenReturn(tipo);
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.importar("contenido");

        ArgumentCaptor<DocumentoTributarioEntity> captor =
                ArgumentCaptor.forClass(DocumentoTributarioEntity.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoTributarioEntity guardado = captor.getValue();

        assertThat(resultado).isSameAs(guardado);
        assertThat(guardado.getTipoDocumento()).isSameAs(tipo);
        assertThat(guardado.getCliente()).isSameAs(cliente);
        assertThat(guardado.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(guardado.getEstadoSii()).isEqualTo(EstadoDocumentoSii.PENDIENTE);
        assertThat(guardado.getXmlFirmado()).isFalse();
        assertThat(guardado.getFolio()).isNull();
        assertThat(guardado.getMoneda()).isEqualTo(Moneda.CLP);
        assertThat(guardado.getTipoCambio()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(guardado.getFechaEmision()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(guardado.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(guardado.getObservaciones()).isEqualTo("Observacion de prueba");
        assertThat(guardado.getDetalles()).hasSize(1);
        DetalleDocumentoEntity detalle = guardado.getDetalles().get(0);
        assertThat(detalle.getDocumento()).isSameAs(guardado);
        assertThat(detalle.getDescripcionItem()).isEqualTo("P001 - Producto uno");
        assertThat(detalle.getCantidad()).isEqualByComparingTo("2");
        assertThat(detalle.getPrecioUnitario()).isEqualByComparingTo("100");

        verify(calculoMontosService).recalcular(guardado);
    }

    @Test
    void importar_clienteNoEncontrado_lanzaReglaNegocioException() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importar("contenido"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se encontró un cliente con razón social 'ACME SPA'");

        verify(tipoDocumentoService, never()).findByCodigoSii(anyInt());
        verify(documentoRepository, never()).save(any());
        verifyNoInteractions(calculoMontosService);
    }

    @Test
    void importar_tipoDocumentoNoExiste_propagaRecursoNoEncontrado() {
        FacturaTxt factura = facturaConDetalles("CLP", List.of(
                detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(tipoDocumentoService.findByCodigoSii(33))
                .thenThrow(new RecursoNoEncontradoException("Tipo de documento no existe: 33"));

        assertThatThrownBy(() -> service.importar("contenido"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Tipo de documento no existe: 33");

        verify(documentoRepository, never()).save(any());
        verifyNoInteractions(calculoMontosService);
    }

    @Test
    void importar_monedaExtranjera_exentaIvaYTotalIgualNeto() {
        FacturaTxt factura = facturaConDetalles("USD", List.of(
                detalle(1, "P001", "Producto USD", new BigDecimal("1"), new BigDecimal("1000"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(tipoDocumentoService.findByCodigoSii(33)).thenReturn(tipoDocumento(33));
        // recalcular() es void: simula que pobló neto/iva/total con IVA aplicado.
        doAnswer(inv -> {
            DocumentoTributarioEntity doc = inv.getArgument(0);
            doc.setMontoNeto(new BigDecimal("1000"));
            doc.setMontoIva(new BigDecimal("190"));
            doc.setMontoTotal(new BigDecimal("1190"));
            return null;
        }).when(calculoMontosService).recalcular(any(DocumentoTributarioEntity.class));
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.importar("contenido");

        // Moneda extranjera: tras recalcular se fuerza IVA cero y total = neto.
        assertThat(resultado.getMoneda()).isEqualTo(Moneda.USD);
        assertThat(resultado.getMontoNeto()).isEqualByComparingTo("1000");
        assertThat(resultado.getMontoIva()).isEqualByComparingTo("0");
        assertThat(resultado.getMontoTotal()).isEqualByComparingTo("1000");
        verify(calculoMontosService).recalcular(resultado);
    }

    @Test
    void importar_fechaEmisionNula_usaFechaActual() {
        FacturaTxt factura = new FacturaTxt(33, cliente("ACME SPA"),
                null, null, "CONTADO", null, "CLP", null, null, null,
                List.of(detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(tipoDocumentoService.findByCodigoSii(33)).thenReturn(tipoDocumento(33));
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.importar("contenido");

        // Sin fecha de emisión en el TXT, se usa hoy y tipoCambio por defecto = 1.
        assertThat(resultado.getFechaEmision()).isEqualTo(LocalDate.now());
        assertThat(resultado.getFechaVencimiento()).isNull();
        assertThat(resultado.getTipoCambio()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void importar_monedaInvalida_caeAClpPorDefecto() {
        FacturaTxt factura = facturaConDetalles("XYZ", List.of(
                detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(tipoDocumentoService.findByCodigoSii(33)).thenReturn(tipoDocumento(33));
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.importar("contenido");

        // Moneda desconocida -> CLP (y por tanto no se fuerza exención).
        assertThat(resultado.getMoneda()).isEqualTo(Moneda.CLP);
        verify(calculoMontosService).recalcular(resultado);
    }

    @Test
    void importar_monedaNula_caeAClpPorDefecto() {
        FacturaTxt factura = facturaConDetalles(null, List.of(
                detalle(1, "P001", "Producto", new BigDecimal("1"), new BigDecimal("100"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(tipoDocumentoService.findByCodigoSii(33)).thenReturn(tipoDocumento(33));
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.importar("contenido");

        assertThat(resultado.getMoneda()).isEqualTo(Moneda.CLP);
    }

    @Test
    void importar_tipoCambioPresente_seRespeta() {
        FacturaTxt factura = new FacturaTxt(33, cliente("ACME SPA"),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1), "CONTADO",
                new BigDecimal("950.5"), "USD", "obs", null, null,
                List.of(detalle(1, "P001", "Producto USD", new BigDecimal("1"), new BigDecimal("1000"))));
        when(parser.parse("contenido")).thenReturn(factura);
        when(clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(EMPRESA_ID, "ACME SPA"))
                .thenReturn(Optional.of(clienteEntity(1L)));
        when(tipoDocumentoService.findByCodigoSii(33)).thenReturn(tipoDocumento(33));
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.importar("contenido");

        assertThat(resultado.getTipoCambio()).isEqualByComparingTo("950.5");
        assertThat(resultado.getMoneda()).isEqualTo(Moneda.USD);
    }

    @Test
    void importar_parserLanza_propagaSinTocarColaboradores() {
        when(parser.parse("malo")).thenThrow(new ReglaNegocioException("El TXT no contiene líneas de **DETALLE**"));

        assertThatThrownBy(() -> service.importar("malo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("DETALLE");

        verifyNoInteractions(clienteRepository, tipoDocumentoService, documentoRepository, calculoMontosService);
    }
}
