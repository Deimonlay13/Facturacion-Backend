package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.documento.DetalleCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoUpdateRequest;
import com.gdl.facturacion_backend.dto.documento.GuiaDespachoRequest;
import com.gdl.facturacion_backend.dto.documento.ReferenciaCreateRequest;
import com.gdl.facturacion_backend.dto.documento.importacion.ImportTxtPreviewResponse;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.Moneda;
import com.gdl.facturacion_backend.enums.TipoTraslado;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import com.gdl.facturacion_backend.service.documento.DocumentoExportService;
import com.gdl.facturacion_backend.service.documento.ImportacionTxtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentoTributarioControllerTest {

    @Mock
    private DocumentoTributarioService service;

    @Mock
    private ImportacionTxtService importacionService;

    @Mock
    private DocumentoExportService exportService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        DocumentoTributarioController controller =
                new DocumentoTributarioController(service, importacionService, exportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------- fixtures

    private TipoDocumentoEntity tipo(int codigoSii, String descripcion) {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(1L);
        tipo.setCodigoSii(codigoSii);
        tipo.setDescripcion(descripcion);
        return tipo;
    }

    private ClienteEntity cliente() {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setId(50L);
        cliente.setRut("76.111.222-3");
        cliente.setRazonSocial("Cliente Demo SpA");
        cliente.setGiro("Servicios");
        cliente.setDireccion("Av. Siempre Viva 123");
        cliente.setComuna("Santiago");
        cliente.setCiudad("Santiago");
        return cliente;
    }

    /** Documento base poblado para que los DTO .from() no fallen. */
    private DocumentoTributarioEntity documento() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setId(10L);
        doc.setTipoDocumento(tipo(33, "Factura Electrónica"));
        doc.setCliente(cliente());
        doc.setFolio(null);
        doc.setFechaEmision(LocalDate.of(2026, 1, 15));
        doc.setFechaVencimiento(LocalDate.of(2026, 2, 15));
        doc.setObservaciones("Sin observaciones");
        doc.setMoneda(Moneda.CLP);
        doc.setTipoCambio(BigDecimal.ONE);
        doc.setEstado(EstadoDocumento.BORRADOR);
        doc.setEstadoSii(EstadoDocumentoSii.PENDIENTE);
        doc.setMontoNeto(new BigDecimal("1000"));
        doc.setMontoIva(new BigDecimal("190"));
        doc.setMontoTotal(new BigDecimal("1190"));
        doc.setCreatedAt(OffsetDateTime.parse("2026-01-15T10:00:00Z"));
        doc.setUpdatedAt(OffsetDateTime.parse("2026-01-15T11:00:00Z"));
        doc.setDetalles(new ArrayList<>());
        doc.setReferencias(new ArrayList<>());
        return doc;
    }

    private DetalleDocumentoEntity detalle() {
        DetalleDocumentoEntity detalle = new DetalleDocumentoEntity();
        detalle.setId(200L);
        detalle.setDescripcionItem("Servicio de consultoría");
        detalle.setCantidad(new BigDecimal("2"));
        detalle.setUnidadMedida("UN");
        detalle.setPrecioUnitario(new BigDecimal("500"));
        detalle.setSubtotal(new BigDecimal("1000"));
        return detalle;
    }

    private ReferenciaDocumentoEntity referencia() {
        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setId(300L);
        DocumentoTributarioEntity referenciado = new DocumentoTributarioEntity();
        referenciado.setId(99L);
        referenciado.setFolio(1234);
        ref.setDocumentoReferenciado(referenciado);
        ref.setTipoReferencia("1");
        ref.setMotivo("Anula factura previa");
        return ref;
    }

    private GuiaDespachoExtraEntity guia() {
        GuiaDespachoExtraEntity guia = new GuiaDespachoExtraEntity();
        guia.setId(400L);
        guia.setTipoTraslado(TipoTraslado.VENTA);
        guia.setPatente("ABCD12");
        guia.setRutTransportista("11.111.111-1");
        guia.setNombreTransportista("Transportes XYZ");
        guia.setDireccionDestino("Bodega Central 456");
        return guia;
    }

    private DocumentoCreateRequest createRequest() {
        DocumentoCreateRequest request = new DocumentoCreateRequest();
        request.setCodigoTipoDocumento(33);
        request.setClienteId(50L);
        return request;
    }

    // --------------------------------------------------- POST /api/documentos

    @Test
    void crear_devuelve201YDocumento() throws Exception {
        when(service.crear(any(DocumentoCreateRequest.class))).thenReturn(documento());

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigoTipoDocumento").value(33))
                .andExpect(jsonPath("$.tipoDocumento").value("Factura Electrónica"))
                .andExpect(jsonPath("$.clienteId").value(50))
                .andExpect(jsonPath("$.clienteRazonSocial").value("Cliente Demo SpA"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.moneda").value("CLP"));

        verify(service).crear(any(DocumentoCreateRequest.class));
    }

    @Test
    void crear_pasaElBodyAlServicio() throws Exception {
        when(service.crear(any(DocumentoCreateRequest.class))).thenReturn(documento());

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        ArgumentCaptor<DocumentoCreateRequest> captor =
                ArgumentCaptor.forClass(DocumentoCreateRequest.class);
        verify(service).crear(captor.capture());
        assertThat(captor.getValue().getCodigoTipoDocumento()).isEqualTo(33);
        assertThat(captor.getValue().getClienteId()).isEqualTo(50L);
    }

    @Test
    void crear_cuandoFaltaTipoDocumento_devuelve400Validacion() throws Exception {
        DocumentoCreateRequest invalido = new DocumentoCreateRequest();
        invalido.setClienteId(50L); // falta codigoTipoDocumento

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void crear_cuandoFaltaCliente_devuelve400Validacion() throws Exception {
        DocumentoCreateRequest invalido = new DocumentoCreateRequest();
        invalido.setCodigoTipoDocumento(33); // falta clienteId

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void crear_cuandoServicioLanzaNoEncontrado_devuelve404() throws Exception {
        when(service.crear(any(DocumentoCreateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado con id: 50"));

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Cliente no encontrado con id: 50"));
    }

    // ---------------------------------------------- PUT /api/documentos/{id}

    @Test
    void actualizarBorrador_devuelve200YDetalle() throws Exception {
        DocumentoTributarioEntity actualizado = documento();
        actualizado.setObservaciones("Observación nueva");
        when(service.actualizarBorrador(eq(10L), any(DocumentoUpdateRequest.class)))
                .thenReturn(actualizado);

        DocumentoUpdateRequest request = new DocumentoUpdateRequest();
        request.setObservaciones("Observación nueva");
        request.setMoneda(Moneda.USD);
        request.setTipoCambio(new BigDecimal("950"));

        mockMvc.perform(put("/api/documentos/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento.id").value(10))
                .andExpect(jsonPath("$.documento.observaciones").value("Observación nueva"))
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.referencias").isArray());

        verify(service).actualizarBorrador(eq(10L), any(DocumentoUpdateRequest.class));
    }

    @Test
    void actualizarBorrador_cuandoTipoCambioNoPositivo_devuelve400Validacion() throws Exception {
        DocumentoUpdateRequest request = new DocumentoUpdateRequest();
        request.setTipoCambio(new BigDecimal("-5"));

        mockMvc.perform(put("/api/documentos/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void actualizarBorrador_cuandoDocumentoEmitido_devuelve400ReglaNegocio() throws Exception {
        when(service.actualizarBorrador(eq(10L), any(DocumentoUpdateRequest.class)))
                .thenThrow(new ReglaNegocioException("El documento está EMITIDO y no puede modificarse"));

        mockMvc.perform(put("/api/documentos/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DocumentoUpdateRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El documento está EMITIDO y no puede modificarse"));
    }

    @Test
    void actualizarBorrador_cuandoNoExiste_devuelve404() throws Exception {
        when(service.actualizarBorrador(eq(99L), any(DocumentoUpdateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Documento no encontrado con id: 99"));

        mockMvc.perform(put("/api/documentos/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DocumentoUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Documento no encontrado con id: 99"));
    }

    // ----------------------------------------------- GET /api/documentos

    @Test
    void consultar_devuelve200YListaDeDocumentos() throws Exception {
        DocumentoTributarioEntity otro = documento();
        otro.setId(11L);
        when(service.consultar(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(documento(), otro));

        mockMvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].id").value(11));

        verify(service).consultar(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void consultar_conFiltros_pasaParametrosAlServicio() throws Exception {
        when(service.consultar(eq(50L), eq(33), eq("BORRADOR"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(List.of(documento()));

        mockMvc.perform(get("/api/documentos")
                        .param("clienteId", "50")
                        .param("tipo", "33")
                        .param("estado", "BORRADOR")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).consultar(eq(50L), eq(33), eq("BORRADOR"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void consultar_cuandoNoHayResultados_devuelveListaVacia() throws Exception {
        when(service.consultar(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void consultar_cuandoEstadoInvalido_devuelve400ReglaNegocio() throws Exception {
        when(service.consultar(isNull(), isNull(), eq("XXX"), isNull(), isNull()))
                .thenThrow(new ReglaNegocioException("Estado inválido: XXX"));

        mockMvc.perform(get("/api/documentos").param("estado", "XXX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("Estado inválido: XXX"));
    }

    // ----------------------------------------- GET /api/documentos/{id}

    @Test
    void obtener_devuelve200YDetalleCompleto() throws Exception {
        DocumentoTributarioEntity doc = documento();
        doc.setDetalles(new ArrayList<>(List.of(detalle())));
        doc.setReferencias(new ArrayList<>(List.of(referencia())));
        doc.setGuiaDespacho(guia());
        when(service.obtenerDetalle(10L)).thenReturn(doc);

        mockMvc.perform(get("/api/documentos/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento.id").value(10))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].id").value(200))
                .andExpect(jsonPath("$.detalles[0].descripcionItem").value("Servicio de consultoría"))
                .andExpect(jsonPath("$.referencias.length()").value(1))
                .andExpect(jsonPath("$.referencias[0].documentoReferenciadoId").value(99))
                .andExpect(jsonPath("$.referencias[0].folioReferenciado").value(1234))
                .andExpect(jsonPath("$.guiaDespacho.id").value(400))
                .andExpect(jsonPath("$.guiaDespacho.tipoTraslado").value("VENTA"));

        verify(service).obtenerDetalle(10L);
    }

    @Test
    void obtener_cuandoNoExiste_devuelve404() throws Exception {
        when(service.obtenerDetalle(99L))
                .thenThrow(new RecursoNoEncontradoException("Documento no encontrado con id: 99"));

        mockMvc.perform(get("/api/documentos/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Documento no encontrado con id: 99"));
    }

    // --------------------------------- POST /api/documentos/{id}/detalles

    @Test
    void agregarDetalle_devuelve201YDetalle() throws Exception {
        DocumentoTributarioEntity doc = documento();
        doc.setDetalles(new ArrayList<>(List.of(detalle())));
        when(service.agregarDetalle(eq(10L), any(DetalleCreateRequest.class))).thenReturn(doc);

        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setDescripcion("Servicio de consultoría");
        request.setCantidad(new BigDecimal("2"));
        request.setPrecioUnitario(new BigDecimal("500"));

        mockMvc.perform(post("/api/documentos/{id}/detalles", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento.id").value(10))
                .andExpect(jsonPath("$.detalles[0].id").value(200))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(1000));

        verify(service).agregarDetalle(eq(10L), any(DetalleCreateRequest.class));
    }

    @Test
    void agregarDetalle_cuandoFaltaDescripcion_devuelve400Validacion() throws Exception {
        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setCantidad(new BigDecimal("2"));
        request.setPrecioUnitario(new BigDecimal("500"));

        mockMvc.perform(post("/api/documentos/{id}/detalles", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void agregarDetalle_cuandoCantidadNoPositiva_devuelve400Validacion() throws Exception {
        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setDescripcion("Item");
        request.setCantidad(BigDecimal.ZERO);
        request.setPrecioUnitario(new BigDecimal("500"));

        mockMvc.perform(post("/api/documentos/{id}/detalles", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void agregarDetalle_cuandoProductoNoExiste_devuelve404() throws Exception {
        when(service.agregarDetalle(eq(10L), any(DetalleCreateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado con id: 7"));

        DetalleCreateRequest request = new DetalleCreateRequest();
        request.setDescripcion("Item");
        request.setCantidad(new BigDecimal("1"));
        request.setPrecioUnitario(new BigDecimal("100"));

        mockMvc.perform(post("/api/documentos/{id}/detalles", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado con id: 7"));
    }

    // --------------------------- DELETE /api/documentos/{id}/detalles/{detalleId}

    @Test
    void eliminarDetalle_devuelve200YDetalleSinLinea() throws Exception {
        when(service.eliminarDetalle(10L, 200L)).thenReturn(documento());

        mockMvc.perform(delete("/api/documentos/{id}/detalles/{detalleId}", 10L, 200L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento.id").value(10))
                .andExpect(jsonPath("$.detalles").isArray());

        verify(service).eliminarDetalle(10L, 200L);
    }

    @Test
    void eliminarDetalle_cuandoNoExiste_devuelve404() throws Exception {
        when(service.eliminarDetalle(10L, 999L))
                .thenThrow(new RecursoNoEncontradoException("Detalle no encontrado con id: 999"));

        mockMvc.perform(delete("/api/documentos/{id}/detalles/{detalleId}", 10L, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Detalle no encontrado con id: 999"));
    }

    // ------------------------------ POST /api/documentos/{id}/referencias

    @Test
    void agregarReferencia_devuelve201YReferencia() throws Exception {
        DocumentoTributarioEntity doc = documento();
        doc.setReferencias(new ArrayList<>(List.of(referencia())));
        when(service.agregarReferencia(eq(10L), any(ReferenciaCreateRequest.class))).thenReturn(doc);

        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(99L);
        request.setTipoReferencia("1");
        request.setMotivo("Anula factura previa");

        mockMvc.perform(post("/api/documentos/{id}/referencias", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referencias[0].id").value(300))
                .andExpect(jsonPath("$.referencias[0].motivo").value("Anula factura previa"));

        verify(service).agregarReferencia(eq(10L), any(ReferenciaCreateRequest.class));
    }

    @Test
    void agregarReferencia_cuandoFaltaMotivo_devuelve400Validacion() throws Exception {
        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(99L); // falta motivo

        mockMvc.perform(post("/api/documentos/{id}/referencias", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void agregarReferencia_cuandoSeReferenciaASiMismo_devuelve400ReglaNegocio() throws Exception {
        when(service.agregarReferencia(eq(10L), any(ReferenciaCreateRequest.class)))
                .thenThrow(new ReglaNegocioException("Un documento no puede referenciarse a sí mismo"));

        ReferenciaCreateRequest request = new ReferenciaCreateRequest();
        request.setDocumentoReferenciadoId(10L);
        request.setMotivo("Motivo");

        mockMvc.perform(post("/api/documentos/{id}/referencias", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Un documento no puede referenciarse a sí mismo"));
    }

    // ------------------------------ PUT /api/documentos/{id}/guia-despacho

    @Test
    void guardarGuiaDespacho_devuelve200YGuia() throws Exception {
        DocumentoTributarioEntity doc = documento();
        doc.setTipoDocumento(tipo(52, "Guía de Despacho"));
        doc.setGuiaDespacho(guia());
        when(service.guardarGuiaDespacho(eq(10L), any(GuiaDespachoRequest.class))).thenReturn(doc);

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA);
        request.setPatente("ABCD12");
        request.setRutTransportista("11.111.111-1");
        request.setNombreTransportista("Transportes XYZ");
        request.setDireccionDestino("Bodega Central 456");

        mockMvc.perform(put("/api/documentos/{id}/guia-despacho", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guiaDespacho.id").value(400))
                .andExpect(jsonPath("$.guiaDespacho.tipoTraslado").value("VENTA"))
                .andExpect(jsonPath("$.guiaDespacho.patente").value("ABCD12"))
                .andExpect(jsonPath("$.guiaDespacho.direccionDestino").value("Bodega Central 456"));

        verify(service).guardarGuiaDespacho(eq(10L), any(GuiaDespachoRequest.class));
    }

    @Test
    void guardarGuiaDespacho_cuandoFaltaDireccion_devuelve400Validacion() throws Exception {
        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA); // falta direccionDestino

        mockMvc.perform(put("/api/documentos/{id}/guia-despacho", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void guardarGuiaDespacho_cuandoTipoNoEs52_devuelve400ReglaNegocio() throws Exception {
        when(service.guardarGuiaDespacho(eq(10L), any(GuiaDespachoRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "Los datos de guía de despacho solo aplican a documentos tipo 52"));

        GuiaDespachoRequest request = new GuiaDespachoRequest();
        request.setTipoTraslado(TipoTraslado.VENTA);
        request.setDireccionDestino("Destino");

        mockMvc.perform(put("/api/documentos/{id}/guia-despacho", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("Los datos de guía de despacho solo aplican a documentos tipo 52"));
    }

    // ----------------------------------- POST /api/documentos/{id}/emitir

    @Test
    void emitir_devuelve200YDocumentoEmitido() throws Exception {
        DocumentoTributarioEntity emitido = documento();
        emitido.setEstado(EstadoDocumento.EMITIDO);
        emitido.setFolio(5001);
        when(service.emitir(10L)).thenReturn(emitido);

        mockMvc.perform(post("/api/documentos/{id}/emitir", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento.id").value(10))
                .andExpect(jsonPath("$.documento.estado").value("EMITIDO"))
                .andExpect(jsonPath("$.documento.folio").value(5001));

        verify(service).emitir(10L);
    }

    @Test
    void emitir_cuandoSinCliente_devuelve400ReglaNegocio() throws Exception {
        when(service.emitir(10L))
                .thenThrow(new ReglaNegocioException("El documento debe tener un cliente asociado"));

        mockMvc.perform(post("/api/documentos/{id}/emitir", 10L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El documento debe tener un cliente asociado"));
    }

    @Test
    void emitir_cuandoNoExiste_devuelve404() throws Exception {
        when(service.emitir(99L))
                .thenThrow(new RecursoNoEncontradoException("Documento no encontrado con id: 99"));

        mockMvc.perform(post("/api/documentos/{id}/emitir", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Documento no encontrado con id: 99"));
    }

    // ----------------------- POST /api/documentos/importar-txt/preview

    @Test
    void previsualizarTxt_devuelve200YPreview() throws Exception {
        ImportTxtPreviewResponse preview = new ImportTxtPreviewResponse(
                33, 50L, "Cliente Demo SpA", true,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15),
                "30 días", "CLP", BigDecimal.ONE, "obs",
                List.of(new ImportTxtPreviewResponse.DetallePreview(
                        1, "P-001", "Item", new BigDecimal("2"),
                        new BigDecimal("500"), new BigDecimal("1000"))),
                new BigDecimal("1000"), new BigDecimal("190"), new BigDecimal("1190"),
                List.of());
        when(importacionService.previsualizar(any(String.class))).thenReturn(preview);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "factura.txt", MediaType.TEXT_PLAIN_VALUE,
                "contenido txt".getBytes(StandardCharsets.ISO_8859_1));

        mockMvc.perform(multipart("/api/documentos/importar-txt/preview").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoTipoDocumento").value(33))
                .andExpect(jsonPath("$.clienteId").value(50))
                .andExpect(jsonPath("$.clienteEncontrado").value(true))
                .andExpect(jsonPath("$.montoTotal").value(1190))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].descripcion").value("Item"));

        verify(importacionService).previsualizar(any(String.class));
    }

    @Test
    void previsualizarTxt_cuandoArchivoVacio_devuelve400ReglaNegocio() throws Exception {
        MockMultipartFile vacio = new MockMultipartFile(
                "archivo", "vacio.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/documentos/importar-txt/preview").file(vacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El archivo TXT está vacío"));
    }

    // ----------------------------- POST /api/documentos/importar-txt

    @Test
    void importarTxt_devuelve201YDocumento() throws Exception {
        DocumentoTributarioEntity doc = documento();
        doc.setDetalles(new ArrayList<>(List.of(detalle())));
        when(importacionService.importar(any(String.class))).thenReturn(doc);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "factura.txt", MediaType.TEXT_PLAIN_VALUE,
                "contenido txt".getBytes(StandardCharsets.ISO_8859_1));

        mockMvc.perform(multipart("/api/documentos/importar-txt").file(archivo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento.id").value(10))
                .andExpect(jsonPath("$.detalles[0].id").value(200));

        verify(importacionService).importar(any(String.class));
    }

    @Test
    void importarTxt_cuandoClienteNoExiste_devuelve400ReglaNegocio() throws Exception {
        when(importacionService.importar(any(String.class)))
                .thenThrow(new ReglaNegocioException(
                        "No se encontró un cliente con razón social 'X'. Créalo antes de importar el TXT."));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "factura.txt", MediaType.TEXT_PLAIN_VALUE,
                "contenido".getBytes(StandardCharsets.ISO_8859_1));

        mockMvc.perform(multipart("/api/documentos/importar-txt").file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Regla de negocio"));
    }

    @Test
    void importarTxt_cuandoArchivoVacio_devuelve400ReglaNegocio() throws Exception {
        MockMultipartFile vacio = new MockMultipartFile(
                "archivo", "vacio.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/documentos/importar-txt").file(vacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El archivo TXT está vacío"));
    }

    // ----------------------------------- GET /api/documentos/{id}/pdf

    @Test
    void descargarPdf_devuelve200ConBytesYHeaders() throws Exception {
        byte[] contenido = "%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8);
        when(exportService.generarPdf(10L)).thenReturn(contenido);

        mockMvc.perform(get("/api/documentos/{id}/pdf", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("documento-10.pdf")))
                .andExpect(content().bytes(contenido));

        verify(exportService).generarPdf(10L);
    }

    @Test
    void descargarPdf_cuandoNoExiste_devuelve404() throws Exception {
        when(exportService.generarPdf(99L))
                .thenThrow(new RecursoNoEncontradoException("Documento no encontrado con id: 99"));

        mockMvc.perform(get("/api/documentos/{id}/pdf", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Documento no encontrado con id: 99"));
    }

    // ----------------------------------- GET /api/documentos/{id}/xml

    @Test
    void descargarXml_devuelve200ConBytesYHeaders() throws Exception {
        byte[] contenido = "<DTE/>".getBytes(StandardCharsets.UTF_8);
        when(exportService.generarXml(10L)).thenReturn(contenido);

        mockMvc.perform(get("/api/documentos/{id}/xml", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("documento-10.xml")))
                .andExpect(content().bytes(contenido));

        verify(exportService).generarXml(10L);
    }

    @Test
    void descargarXml_cuandoFallaGeneracion_devuelve500() throws Exception {
        when(exportService.generarXml(10L))
                .thenThrow(new RuntimeException("No se pudo generar el XML del documento"));

        mockMvc.perform(get("/api/documentos/{id}/xml", 10L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno"));
    }
}
