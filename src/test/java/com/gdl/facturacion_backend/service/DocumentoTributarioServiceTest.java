package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.documento.DetalleCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoUpdateRequest;
import com.gdl.facturacion_backend.dto.documento.GuiaDespachoRequest;
import com.gdl.facturacion_backend.dto.documento.ReferenciaCreateRequest;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.Moneda;
import com.gdl.facturacion_backend.enums.TipoTraslado;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.repository.EmpresaRepository;
import com.gdl.facturacion_backend.repository.GuiaDespachoExtraRepository;
import com.gdl.facturacion_backend.repository.ProductoRepository;
import com.gdl.facturacion_backend.repository.ReferenciaDocumentoRepository;
import com.gdl.facturacion_backend.service.documento.CalculoMontosService;
import com.gdl.facturacion_backend.service.documento.ReglaTributariaResolver;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoTributarioServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final int CODIGO_FACTURA = 33;
    private static final int CODIGO_GUIA = 52;

    @Mock
    private DocumentoTributarioRepository documentoRepository;
    @Mock
    private TenantService tenantService;
    @Mock
    private TipoDocumentoService tipoDocumentoService;
    @Mock
    private ClienteService clienteService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ReferenciaDocumentoRepository referenciaRepository;
    @Mock
    private GuiaDespachoExtraRepository guiaRepository;
    @Mock
    private CalculoMontosService calculoMontosService;
    @Mock
    private ReglaTributariaResolver reglaResolver;
    @Mock
    private FolioService folioService;
    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private DocumentoTributarioService service;

    @BeforeEach
    void setUp() {
        // El servicio hereda getEmpresaId() de BaseTenantService -> tenantService.getEmpresaId().
        // lenient porque algunos tests cortan antes de tocar el tenant.
        lenient().when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
    }

    // ------------------------------------------------------------------ helpers

    private TipoDocumentoEntity tipo(int codigoSii) {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId((long) codigoSii);
        tipo.setCodigoSii(codigoSii);
        tipo.setDescripcion("Tipo " + codigoSii);
        return tipo;
    }

    private ClienteEntity cliente(Long id) {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setId(id);
        cliente.setRut("12345678-5");
        cliente.setRazonSocial("Cliente SpA");
        cliente.setGiro("Servicios");
        cliente.setDireccion("Calle 1");
        cliente.setCiudad("Santiago");
        cliente.setComuna("Providencia");
        cliente.setPais("Chile");
        cliente.setEmail("cliente@test.cl");
        return cliente;
    }

    private DocumentoTributarioEntity documento(Long id, EstadoDocumento estado, TipoDocumentoEntity tipo) {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setId(id);
        doc.setEstado(estado);
        doc.setTipoDocumento(tipo);
        doc.setCliente(cliente(50L));
        return doc;
    }

    /** Hace que findByIdAndEmpresaId(id, EMPRESA_ID) devuelva el documento (se invoca varias veces). */
    private void stubFind(DocumentoTributarioEntity doc) {
        when(documentoRepository.findByIdAndEmpresaId(doc.getId(), EMPRESA_ID))
                .thenReturn(Optional.of(doc));
    }

    /** documentoRepository.save devuelve la misma instancia recibida. */
    private void stubSaveEcho() {
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_inicializaBorradorConValoresPorDefecto() {
        DocumentoCreateRequest request = new DocumentoCreateRequest();
        request.setCodigoTipoDocumento(CODIGO_FACTURA);
        request.setClienteId(50L);

        TipoDocumentoEntity tipo = tipo(CODIGO_FACTURA);
        ClienteEntity cliente = cliente(50L);
        when(tipoDocumentoService.findByCodigoSii(CODIGO_FACTURA)).thenReturn(tipo);
        when(clienteService.obtenerPorId(50L)).thenReturn(cliente);
        stubSaveEcho();

        DocumentoTributarioEntity resultado = service.crear(request);

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
        assertThat(guardado.getFechaEmision()).isEqualTo(LocalDate.now());
        // save() de la base asigna la empresa del tenant.
        assertThat(guardado.getEmpresa()).isNotNull();
        assertThat(guardado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void crear_tipoInexistente_propagaRecursoNoEncontrado() {
        DocumentoCreateRequest request = new DocumentoCreateRequest();
        request.setCodigoTipoDocumento(999);
        request.setClienteId(50L);
        when(tipoDocumentoService.findByCodigoSii(999))
                .thenThrow(new RecursoNoEncontradoException("Tipo de documento no existe: 999"));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void crear_clienteInexistente_propagaRecursoNoEncontrado() {
        DocumentoCreateRequest request = new DocumentoCreateRequest();
        request.setCodigoTipoDocumento(CODIGO_FACTURA);
        request.setClienteId(99L);
        when(tipoDocumentoService.findByCodigoSii(CODIGO_FACTURA)).thenReturn(tipo(CODIGO_FACTURA));
        when(clienteService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado con id: 99"));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(documentoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ actualizarBorrador

    @Test
    void actualizarBorrador_aplicaTodosLosCamposPresentes() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        stubFind(doc);
        stubSaveEcho();

        ClienteEntity nuevoCliente = cliente(60L);
        when(clienteService.obtenerPorId(60L)).thenReturn(nuevoCliente);

        DocumentoUpdateRequest request = new DocumentoUpdateRequest();
        request.setClienteId(60L);
        request.setFechaVencimiento(LocalDate.of(2026, 1, 1));
        request.setObservaciones("Nota");
        request.setMoneda(Moneda.USD);
        request.setTipoCambio(new BigDecimal("950.5"));

        DocumentoTributarioEntity resultado = service.actualizarBorrador(10L, request);

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getCliente()).isSameAs(nuevoCliente);
        assertThat(doc.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(doc.getObservaciones()).isEqualTo("Nota");
        assertThat(doc.getMoneda()).isEqualTo(Moneda.USD);
        assertThat(doc.getTipoCambio()).isEqualByComparingTo("950.5");
        verify(documentoRepository).save(doc);
    }

    @Test
    void actualizarBorrador_camposNulos_noModificaNiConsultaCliente() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        doc.setObservaciones("Original");
        doc.setMoneda(Moneda.CLP);
        stubFind(doc);
        stubSaveEcho();

        DocumentoTributarioEntity resultado = service.actualizarBorrador(10L, new DocumentoUpdateRequest());

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getObservaciones()).isEqualTo("Original");
        assertThat(doc.getMoneda()).isEqualTo(Moneda.CLP);
        verify(clienteService, never()).obtenerPorId(any());
        verify(documentoRepository).save(doc);
    }

    @Test
    void actualizarBorrador_documentoInexistente_lanzaRecursoNoEncontrado() {
        when(documentoRepository.findByIdAndEmpresaId(77L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarBorrador(77L, new DocumentoUpdateRequest()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Documento no encontrado con id: 77");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void actualizarBorrador_documentoEmitido_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.actualizarBorrador(10L, new DocumentoUpdateRequest()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("EMITIDO");

        verify(documentoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ consultar

    @Test
    void consultar_delegaEnRepositorioConEstadoParseado() {
        DocumentoTributarioEntity doc = documento(1L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        when(documentoRepository.buscar(EMPRESA_ID, 50L, CODIGO_FACTURA,
                EstadoDocumento.EMITIDO, desde, hasta)).thenReturn(List.of(doc));

        List<DocumentoTributarioEntity> resultado =
                service.consultar(50L, CODIGO_FACTURA, "emitido", desde, hasta);

        assertThat(resultado).containsExactly(doc);
        verify(documentoRepository).buscar(EMPRESA_ID, 50L, CODIGO_FACTURA,
                EstadoDocumento.EMITIDO, desde, hasta);
    }

    @Test
    void consultar_estadoNulo_pasaNullAlRepositorio() {
        when(documentoRepository.buscar(EMPRESA_ID, null, null, null, null, null))
                .thenReturn(List.of());

        List<DocumentoTributarioEntity> resultado =
                service.consultar(null, null, null, null, null);

        assertThat(resultado).isEmpty();
        verify(documentoRepository).buscar(EMPRESA_ID, null, null, null, null, null);
    }

    @Test
    void consultar_estadoEnBlanco_pasaNullAlRepositorio() {
        when(documentoRepository.buscar(EMPRESA_ID, null, null, null, null, null))
                .thenReturn(List.of());

        service.consultar(null, null, "   ", null, null);

        verify(documentoRepository).buscar(EMPRESA_ID, null, null, null, null, null);
    }

    @Test
    void consultar_estadoInvalido_lanzaReglaNegocio() {
        assertThatThrownBy(() -> service.consultar(null, null, "PAGADO", null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Estado inválido: PAGADO");

        verifyNoInteractions(documentoRepository);
    }

    // ------------------------------------------------------------------ obtenerDetalle / obtenerPorId

    @Test
    void obtenerDetalle_existente_cargaYPrecargaColecciones() {
        DocumentoTributarioEntity doc = documento(5L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        doc.setDetalles(new ArrayList<>(List.of(new DetalleDocumentoEntity())));
        doc.setReferencias(new ArrayList<>(List.of(new ReferenciaDocumentoEntity())));
        when(documentoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        DocumentoTributarioEntity resultado = service.obtenerDetalle(5L);

        assertThat(resultado).isSameAs(doc);
    }

    @Test
    void obtenerDetalle_inexistente_lanzaRecursoNoEncontrado() {
        when(documentoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDetalle(5L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Documento no encontrado con id: 5");
    }

    @Test
    void obtenerPorId_existente_devuelveDocumento() {
        DocumentoTributarioEntity doc = documento(5L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThat(service.obtenerPorId(5L)).isSameAs(doc);
    }

    @Test
    void obtenerPorId_inexistente_lanzaRecursoNoEncontrado() {
        when(documentoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(5L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ------------------------------------------------------------------ agregarDetalle

    @Test
    void agregarDetalle_conProducto_creaDetalleYRecalcula() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        stubFind(doc);
        stubSaveEcho();

        ProductoEntity producto = new ProductoEntity();
        producto.setId(7L);
        when(productoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(calculoMontosService.calcularSubtotal(any(DetalleDocumentoEntity.class)))
                .thenReturn(new BigDecimal("200"));

        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setProductoId(7L);
        request.setDescripcion("Item con producto");
        request.setCantidad(new BigDecimal("2"));
        request.setUnidadMedida("UN");
        request.setPrecioUnitario(new BigDecimal("100"));

        DocumentoTributarioEntity resultado = service.agregarDetalle(10L, request);

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getDetalles()).hasSize(1);
        DetalleDocumentoEntity detalle = doc.getDetalles().get(0);
        assertThat(detalle.getProducto()).isSameAs(producto);
        assertThat(detalle.getDocumento()).isSameAs(doc);
        assertThat(detalle.getDescripcionItem()).isEqualTo("Item con producto");
        assertThat(detalle.getCantidad()).isEqualByComparingTo("2");
        assertThat(detalle.getUnidadMedida()).isEqualTo("UN");
        assertThat(detalle.getPrecioUnitario()).isEqualByComparingTo("100");
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("200");
        verify(calculoMontosService).recalcular(doc);
        verify(documentoRepository).save(doc);
    }

    @Test
    void agregarDetalle_sinProducto_noConsultaProductoRepository() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        stubFind(doc);
        stubSaveEcho();
        when(calculoMontosService.calcularSubtotal(any(DetalleDocumentoEntity.class)))
                .thenReturn(new BigDecimal("50"));

        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setDescripcion("Item libre");
        request.setCantidad(new BigDecimal("1"));
        request.setPrecioUnitario(new BigDecimal("50"));

        service.agregarDetalle(10L, request);

        assertThat(doc.getDetalles()).hasSize(1);
        assertThat(doc.getDetalles().get(0).getProducto()).isNull();
        verify(productoRepository, never()).findByIdAndEmpresaId(any(), any());
        verify(calculoMontosService).recalcular(doc);
    }

    @Test
    void agregarDetalle_productoNoEncontrado_lanzaRecursoNoEncontrado() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));
        when(productoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.empty());

        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setProductoId(7L);
        request.setDescripcion("X");
        request.setCantidad(BigDecimal.ONE);
        request.setPrecioUnitario(BigDecimal.TEN);

        assertThatThrownBy(() -> service.agregarDetalle(10L, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Producto no encontrado con id: 7");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void agregarDetalle_documentoEmitido_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setDescripcion("X");
        request.setCantidad(BigDecimal.ONE);
        request.setPrecioUnitario(BigDecimal.TEN);

        assertThatThrownBy(() -> service.agregarDetalle(10L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("EMITIDO");

        verifyNoInteractions(productoRepository, calculoMontosService);
        verify(documentoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ eliminarDetalle

    @Test
    void eliminarDetalle_existente_eliminaYRecalcula() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        DetalleDocumentoEntity detalle = new DetalleDocumentoEntity();
        detalle.setId(3L);
        doc.setDetalles(new ArrayList<>(List.of(detalle)));
        stubFind(doc);
        stubSaveEcho();

        DocumentoTributarioEntity resultado = service.eliminarDetalle(10L, 3L);

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getDetalles()).isEmpty();
        verify(calculoMontosService).recalcular(doc);
        verify(documentoRepository).save(doc);
    }

    @Test
    void eliminarDetalle_detalleInexistente_lanzaRecursoNoEncontrado() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        DetalleDocumentoEntity detalle = new DetalleDocumentoEntity();
        detalle.setId(3L);
        doc.setDetalles(new ArrayList<>(List.of(detalle)));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.eliminarDetalle(10L, 99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Detalle no encontrado con id: 99");

        verify(calculoMontosService, never()).recalcular(any());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void eliminarDetalle_sinColeccionDetalles_lanzaRecursoNoEncontrado() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        doc.setDetalles(null);
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.eliminarDetalle(10L, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void eliminarDetalle_documentoEmitido_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.eliminarDetalle(10L, 1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("EMITIDO");
    }

    // ------------------------------------------------------------------ agregarReferencia

    @Test
    void agregarReferencia_valida_persisteYAgrega() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        DocumentoTributarioEntity referenciado = documento(20L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));
        when(documentoRepository.findByIdAndEmpresaId(20L, EMPRESA_ID)).thenReturn(Optional.of(referenciado));
        when(referenciaRepository.save(any(ReferenciaDocumentoEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(20L);
        request.setTipoReferencia("REF");
        request.setMotivo("Anula factura");

        DocumentoTributarioEntity resultado = service.agregarReferencia(10L, request);

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getReferencias()).hasSize(1);
        ReferenciaDocumentoEntity ref = doc.getReferencias().get(0);
        assertThat(ref.getDocumento()).isSameAs(doc);
        assertThat(ref.getDocumentoReferenciado()).isSameAs(referenciado);
        assertThat(ref.getTipoReferencia()).isEqualTo("REF");
        assertThat(ref.getMotivo()).isEqualTo("Anula factura");
        verify(referenciaRepository).save(any(ReferenciaDocumentoEntity.class));
    }

    @Test
    void agregarReferencia_aSiMismo_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        // cargarEditable y cargar consultan el mismo id 10.
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(10L);
        request.setMotivo("X");

        assertThatThrownBy(() -> service.agregarReferencia(10L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no puede referenciarse a sí mismo");

        verify(referenciaRepository, never()).save(any());
    }

    @Test
    void agregarReferencia_documentoReferenciadoInexistente_lanzaRecursoNoEncontrado() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));
        when(documentoRepository.findByIdAndEmpresaId(20L, EMPRESA_ID)).thenReturn(Optional.empty());

        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(20L);
        request.setMotivo("X");

        assertThatThrownBy(() -> service.agregarReferencia(10L, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Documento no encontrado con id: 20");

        verify(referenciaRepository, never()).save(any());
    }

    @Test
    void agregarReferencia_documentoEmitido_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(20L);
        request.setMotivo("X");

        assertThatThrownBy(() -> service.agregarReferencia(10L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("EMITIDO");

        verify(referenciaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ guardarGuiaDespacho

    @Test
    void guardarGuiaDespacho_nuevaGuia_creaYAsigna() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_GUIA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));
        when(guiaRepository.findByDocumentoIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());
        when(guiaRepository.save(any(GuiaDespachoExtraEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA);
        request.setPatente("ABCD12");
        request.setRutTransportista("11111111-1");
        request.setNombreTransportista("Transporte SpA");
        request.setDireccionDestino("Bodega 5");

        DocumentoTributarioEntity resultado = service.guardarGuiaDespacho(10L, request);

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getGuiaDespacho()).isNotNull();
        GuiaDespachoExtraEntity guia = doc.getGuiaDespacho();
        assertThat(guia.getDocumento()).isSameAs(doc);
        assertThat(guia.getTipoTraslado()).isEqualTo(TipoTraslado.VENTA);
        assertThat(guia.getPatente()).isEqualTo("ABCD12");
        assertThat(guia.getRutTransportista()).isEqualTo("11111111-1");
        assertThat(guia.getNombreTransportista()).isEqualTo("Transporte SpA");
        assertThat(guia.getDireccionDestino()).isEqualTo("Bodega 5");
        verify(guiaRepository).save(any(GuiaDespachoExtraEntity.class));
    }

    @Test
    void guardarGuiaDespacho_guiaExistente_actualizaSinCrearNueva() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_GUIA));
        GuiaDespachoExtraEntity existente = new GuiaDespachoExtraEntity();
        existente.setId(99L);
        existente.setDocumento(doc);
        existente.setDireccionDestino("Vieja");
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));
        when(guiaRepository.findByDocumentoIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(guiaRepository.save(any(GuiaDespachoExtraEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.TRASLADO_INTERNO);
        request.setDireccionDestino("Nueva");

        service.guardarGuiaDespacho(10L, request);

        assertThat(doc.getGuiaDespacho()).isSameAs(existente);
        assertThat(existente.getDireccionDestino()).isEqualTo("Nueva");
        assertThat(existente.getTipoTraslado()).isEqualTo(TipoTraslado.TRASLADO_INTERNO);
        verify(guiaRepository).save(existente);
    }

    @Test
    void guardarGuiaDespacho_tipoNoEs52_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA);
        request.setDireccionDestino("Bodega");

        assertThatThrownBy(() -> service.guardarGuiaDespacho(10L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tipo 52");

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void guardarGuiaDespacho_sinTipoDocumento_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, null);
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA);
        request.setDireccionDestino("Bodega");

        assertThatThrownBy(() -> service.guardarGuiaDespacho(10L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tipo 52");

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void guardarGuiaDespacho_documentoEmitido_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.EMITIDO, tipo(CODIGO_GUIA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA);
        request.setDireccionDestino("Bodega");

        assertThatThrownBy(() -> service.guardarGuiaDespacho(10L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("EMITIDO");

        verifyNoInteractions(guiaRepository);
    }

    // ------------------------------------------------------------------ emitir

    @Test
    void emitir_documentoValido_asignaFolioYSnapshotsYCambiaEstado() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        stubFind(doc);
        stubSaveEcho();

        ReglaTributaria regla = org.mockito.Mockito.mock(ReglaTributaria.class);
        when(reglaResolver.resolver(CODIGO_FACTURA)).thenReturn(regla);
        when(folioService.asignarSiguienteFolio(CODIGO_FACTURA)).thenReturn(1001);

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(EMPRESA_ID);
        empresa.setRutEmpresa("76000000-0");
        empresa.setRazonSocial("Mi Empresa SpA");
        empresa.setNombreFantasia("MiEmp");
        empresa.setGiro("Comercio");
        empresa.setDireccion("Av 100");
        empresa.setCiudad("Santiago");
        empresa.setComuna("Las Condes");
        empresa.setPais("Chile");
        empresa.setTelefono("+56222222222");
        empresa.setEmailPrincipal("info@miemp.cl");
        empresa.setEmailContabilidad("conta@miemp.cl");
        empresa.setRutRepresentante("12121212-1");
        empresa.setNombreRepresentante("Juan Rep");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));

        DocumentoTributarioEntity resultado = service.emitir(10L);

        assertThat(resultado).isSameAs(doc);
        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.EMITIDO);
        assertThat(doc.getFolio()).isEqualTo(1001);
        assertThat(doc.getFechaEmision()).isEqualTo(LocalDate.now());
        // snapshot cliente
        assertThat(doc.getRut()).isEqualTo("12345678-5");
        assertThat(doc.getRazonSocial()).isEqualTo("Cliente SpA");
        assertThat(doc.getCorreo()).isEqualTo("cliente@test.cl");
        // snapshot empresa
        assertThat(doc.getRutEmisor()).isEqualTo("76000000-0");
        assertThat(doc.getRazonSocialEmisor()).isEqualTo("Mi Empresa SpA");
        assertThat(doc.getNombreFantasiaEmisor()).isEqualTo("MiEmp");
        assertThat(doc.getEmailContabilidadEmisor()).isEqualTo("conta@miemp.cl");
        assertThat(doc.getNombreRepresentanteEmisor()).isEqualTo("Juan Rep");

        verify(regla).validarParaEmitir(doc);
        verify(calculoMontosService).recalcular(doc);
        verify(folioService).asignarSiguienteFolio(CODIGO_FACTURA);
        verify(documentoRepository).save(doc);
    }

    @Test
    void emitir_sinCliente_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        doc.setCliente(null);
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cliente asociado");

        verify(folioService, never()).asignarSiguienteFolio(anyInt());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void emitir_sinTipoDocumento_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, null);
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tipo de documento");

        verify(folioService, never()).asignarSiguienteFolio(anyInt());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void emitir_reglaRechazaValidacion_propagaReglaNegocioYNoAsignaFolio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        ReglaTributaria regla = org.mockito.Mockito.mock(ReglaTributaria.class);
        when(reglaResolver.resolver(CODIGO_FACTURA)).thenReturn(regla);
        org.mockito.Mockito.doThrow(new ReglaNegocioException("El documento debe tener al menos un detalle"))
                .when(regla).validarParaEmitir(doc);

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos un detalle");

        verify(calculoMontosService, never()).recalcular(any());
        verify(folioService, never()).asignarSiguienteFolio(anyInt());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void emitir_empresaNoEncontrada_lanzaRecursoNoEncontrado() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        ReglaTributaria regla = org.mockito.Mockito.mock(ReglaTributaria.class);
        when(reglaResolver.resolver(CODIGO_FACTURA)).thenReturn(regla);
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Empresa no encontrada");

        verify(folioService, never()).asignarSiguienteFolio(anyInt());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void emitir_sinFoliosDisponibles_propagaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        ReglaTributaria regla = org.mockito.Mockito.mock(ReglaTributaria.class);
        when(reglaResolver.resolver(CODIGO_FACTURA)).thenReturn(regla);

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(EMPRESA_ID);
        empresa.setRutEmpresa("76000000-0");
        empresa.setRazonSocial("Mi Empresa SpA");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(folioService.asignarSiguienteFolio(CODIGO_FACTURA))
                .thenThrow(new ReglaNegocioException("No hay folios disponibles para el tipo 33"));

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No hay folios disponibles");

        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void emitir_documentoEmitido_lanzaReglaNegocio() {
        DocumentoTributarioEntity doc = documento(10L, EstadoDocumento.EMITIDO, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("EMITIDO");

        verifyNoInteractions(reglaResolver, folioService);
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void emitir_documentoInexistente_lanzaRecursoNoEncontrado() {
        when(documentoRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.emitir(10L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Documento no encontrado con id: 10");
    }

    // ------------------------------------------------------------------ save (heredado)

    @Test
    void save_entidadConIdInexistente_lanzaRuntimeException() {
        DocumentoTributarioEntity entidad = documento(123L, EstadoDocumento.BORRADOR, tipo(CODIGO_FACTURA));
        when(documentoRepository.findByIdAndEmpresaId(123L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(entidad))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tienes permiso sobre este registro");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void save_entidadSinId_asignaEmpresaYGuarda() {
        DocumentoTributarioEntity entidad = new DocumentoTributarioEntity();
        when(documentoRepository.save(any(DocumentoTributarioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoTributarioEntity resultado = service.save(entidad);

        assertThat(resultado.getEmpresa()).isNotNull();
        assertThat(resultado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
        verify(documentoRepository, never()).findByIdAndEmpresaId(eq(123L), eq(EMPRESA_ID));
        verify(documentoRepository).save(entidad);
    }
}
