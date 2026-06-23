package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gdl.facturacion_backend.dto.folio.CafCargaRequest;
import com.gdl.facturacion_backend.dto.folio.CafResponse;
import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoCaf;
import com.gdl.facturacion_backend.enums.EstadoFolio;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.FolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FolioControllerTest {

    @Mock
    private FolioService service;

    @InjectMocks
    private FolioController controller;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                // Necesario para resolver el Pageable del endpoint GET /api/folios
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers de construcción de entidades / DTOs
    // ------------------------------------------------------------------

    private TipoDocumentoEntity sampleTipo() {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(1L);
        tipo.setCodigoSii(33);
        tipo.setDescripcion("Factura Electrónica");
        return tipo;
    }

    private CafEntity sampleCaf() {
        CafEntity caf = new CafEntity();
        caf.setId(10L);
        caf.setTipoDocumento(sampleTipo());
        caf.setRangoDesde(1);
        caf.setRangoHasta(100);
        caf.setFechaAutorizacion(LocalDate.parse("2026-01-01"));
        caf.setFechaVencimiento(LocalDate.parse("2026-12-31"));
        caf.setCafXml("<CAF>contenido</CAF>");
        caf.setEstado(EstadoCaf.DISPONIBLE);
        return caf;
    }

    private CafResponse sampleCafResponse() {
        // foliosGenerados = 100, foliosDisponibles = 100
        return CafResponse.from(sampleCaf(), 100L, 100L);
    }

    private ControlFolioEntity sampleControl() {
        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(5L);
        control.setTipoDocumento(sampleTipo());
        control.setCafActivo(sampleCaf());
        control.setUltimoFolioUtilizado(42);
        return control;
    }

    private FolioEntity sampleFolio() {
        FolioEntity folio = new FolioEntity();
        folio.setId(200L);
        folio.setCaf(sampleCaf());
        folio.setTipoDocumento(sampleTipo());
        folio.setNumero(1);
        folio.setEstado(EstadoFolio.DISPONIBLE);
        return folio;
    }

    private CafCargaRequest sampleRequest() {
        CafCargaRequest request = new CafCargaRequest();
        request.setCodigoTipoDocumento(33);
        request.setRangoDesde(1);
        request.setRangoHasta(100);
        request.setFechaAutorizacion(LocalDate.parse("2026-01-01"));
        request.setFechaVencimiento(LocalDate.parse("2026-12-31"));
        request.setCafXml("<CAF>contenido</CAF>");
        return request;
    }

    // ------------------------------------------------------------------
    // POST /api/folios/caf -> cargarCaf
    // ------------------------------------------------------------------

    @Test
    void cargarCaf_devuelve201YCafCreado() throws Exception {
        when(service.cargarCaf(any(CafCargaRequest.class))).thenReturn(sampleCafResponse());

        mockMvc.perform(post("/api/folios/caf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigoTipoDocumento").value(33))
                .andExpect(jsonPath("$.tipoDocumento").value("Factura Electrónica"))
                .andExpect(jsonPath("$.rangoDesde").value(1))
                .andExpect(jsonPath("$.rangoHasta").value(100))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.foliosGenerados").value(100))
                .andExpect(jsonPath("$.foliosDisponibles").value(100));

        verify(service).cargarCaf(any(CafCargaRequest.class));
    }

    @Test
    void cargarCaf_pasaLosDatosDelBodyAlServicio() throws Exception {
        when(service.cargarCaf(any(CafCargaRequest.class))).thenReturn(sampleCafResponse());

        mockMvc.perform(post("/api/folios/caf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());

        ArgumentCaptor<CafCargaRequest> captor = ArgumentCaptor.forClass(CafCargaRequest.class);
        verify(service).cargarCaf(captor.capture());

        CafCargaRequest enviado = captor.getValue();
        assertThat(enviado.getCodigoTipoDocumento()).isEqualTo(33);
        assertThat(enviado.getRangoDesde()).isEqualTo(1);
        assertThat(enviado.getRangoHasta()).isEqualTo(100);
        assertThat(enviado.getFechaAutorizacion()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(enviado.getFechaVencimiento()).isEqualTo(LocalDate.parse("2026-12-31"));
        assertThat(enviado.getCafXml()).isEqualTo("<CAF>contenido</CAF>");
    }

    @Test
    void cargarCaf_cuandoRangoSeSuperpone_devuelve400() throws Exception {
        when(service.cargarCaf(any(CafCargaRequest.class)))
                .thenThrow(new ReglaNegocioException("El rango del CAF se superpone con folios ya existentes"));

        mockMvc.perform(post("/api/folios/caf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El rango del CAF se superpone con folios ya existentes"));
    }

    @Test
    void cargarCaf_cuandoServicioLanzaRuntime_devuelve500() throws Exception {
        when(service.cargarCaf(any(CafCargaRequest.class)))
                .thenThrow(new RuntimeException("Fallo inesperado"));

        mockMvc.perform(post("/api/folios/caf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("Fallo inesperado"));
    }

    @Test
    void cargarCaf_cuandoBodyInvalido_devuelve400DeValidacion() throws Exception {
        // Body vacío: dispara las validaciones @NotNull/@NotBlank del request
        mockMvc.perform(post("/api/folios/caf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    // ------------------------------------------------------------------
    // GET /api/folios/caf -> listarCafs
    // ------------------------------------------------------------------

    @Test
    void listarCafs_devuelve200YLista() throws Exception {
        CafEntity otro = sampleCaf();
        otro.setId(11L);
        otro.setRangoDesde(101);
        otro.setRangoHasta(200);
        CafResponse otraResp = CafResponse.from(otro, 100L, 50L);

        when(service.listarCafs()).thenReturn(List.of(sampleCafResponse(), otraResp));

        mockMvc.perform(get("/api/folios/caf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].rangoDesde").value(1))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].rangoDesde").value(101))
                .andExpect(jsonPath("$[1].foliosDisponibles").value(50));

        verify(service).listarCafs();
    }

    @Test
    void listarCafs_cuandoNoHay_devuelveListaVacia() throws Exception {
        when(service.listarCafs()).thenReturn(List.of());

        mockMvc.perform(get("/api/folios/caf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).listarCafs();
    }

    // ------------------------------------------------------------------
    // GET /api/folios/control -> listarControles
    // ------------------------------------------------------------------

    @Test
    void listarControles_devuelve200YListaMapeada() throws Exception {
        when(service.listar()).thenReturn(List.of(sampleControl()));

        mockMvc.perform(get("/api/folios/control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].cafActivoId").value(10))
                .andExpect(jsonPath("$[0].codigoTipoDocumento").value(33))
                .andExpect(jsonPath("$[0].tipoDocumento").value("Factura Electrónica"))
                .andExpect(jsonPath("$[0].rangoDesde").value(1))
                .andExpect(jsonPath("$[0].rangoHasta").value(100))
                .andExpect(jsonPath("$[0].ultimoFolioUtilizado").value(42))
                .andExpect(jsonPath("$[0].estadoCaf").value("DISPONIBLE"));

        verify(service).listar();
    }

    @Test
    void listarControles_cuandoControlSinCaf_mapeaCamposNulos() throws Exception {
        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(6L);
        control.setTipoDocumento(sampleTipo());
        control.setCafActivo(null);
        control.setUltimoFolioUtilizado(null);

        when(service.listar()).thenReturn(List.of(control));

        mockMvc.perform(get("/api/folios/control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6))
                .andExpect(jsonPath("$[0].cafActivoId").doesNotExist())
                .andExpect(jsonPath("$[0].rangoDesde").doesNotExist())
                .andExpect(jsonPath("$[0].estadoCaf").doesNotExist())
                .andExpect(jsonPath("$[0].codigoTipoDocumento").value(33));

        verify(service).listar();
    }

    @Test
    void listarControles_cuandoNoHay_devuelveListaVacia() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/folios/control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).listar();
    }

    // ------------------------------------------------------------------
    // GET /api/folios -> listarFolios (paginado)
    // ------------------------------------------------------------------

    @Test
    void listarFolios_devuelve200YPaginaMapeada() throws Exception {
        FolioEntity folio2 = sampleFolio();
        folio2.setId(201L);
        folio2.setNumero(2);
        folio2.setEstado(EstadoFolio.UTILIZADO);

        Page<FolioEntity> page = new PageImpl<>(
                List.of(sampleFolio(), folio2),
                PageRequest.of(0, 20),
                2);

        when(service.listarFoliosPaginado(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/folios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(200))
                .andExpect(jsonPath("$.content[0].numero").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value(33))
                .andExpect(jsonPath("$.content[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.content[1].id").value(201))
                .andExpect(jsonPath("$.content[1].numero").value(2))
                .andExpect(jsonPath("$.content[1].estado").value("UTILIZADO"));

        verify(service).listarFoliosPaginado(any(Pageable.class));
    }

    @Test
    void listarFolios_respetaParametrosDePaginacion() throws Exception {
        Page<FolioEntity> page = new PageImpl<>(
                List.of(sampleFolio()),
                PageRequest.of(2, 5),
                11);

        when(service.listarFoliosPaginado(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/folios")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(2));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listarFoliosPaginado(captor.capture());

        Pageable enviado = captor.getValue();
        assertThat(enviado.getPageNumber()).isEqualTo(2);
        assertThat(enviado.getPageSize()).isEqualTo(5);
    }

    @Test
    void listarFolios_cuandoNoHay_devuelvePaginaVacia() throws Exception {
        Page<FolioEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(service.listarFoliosPaginado(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/folios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(service).listarFoliosPaginado(any(Pageable.class));
    }
}
