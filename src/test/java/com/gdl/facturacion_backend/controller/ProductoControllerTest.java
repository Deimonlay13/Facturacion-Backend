package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService service;

    @InjectMocks
    private ProductoController controller;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ProductoEntity sampleEntity() {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(7L);
        entity.setCodigo("P-001");
        entity.setNombre("Producto de prueba");
        entity.setDescripcion("Descripción de prueba");
        entity.setUnidadMedida("UN");
        entity.setPrecio(1500.0);
        entity.setAfectaIva(true);
        entity.setActivo(true);
        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T11:00:00Z"));
        return entity;
    }

    private ProductoRequest sampleRequest() {
        ProductoRequest request = new ProductoRequest();
        request.setCodigo("P-001");
        request.setNombre("Producto de prueba");
        request.setDescripcion("Descripción de prueba");
        request.setUnidadMedida("UN");
        request.setPrecio(1500.0);
        request.setAfectaIva(true);
        request.setActivo(true);
        return request;
    }

    // ---------------------------------------------------------------------
    // POST /productos -> create
    // ---------------------------------------------------------------------

    @Test
    void create_devuelve201YProductoCreado() throws Exception {
        when(service.create(any(ProductoRequest.class))).thenReturn(sampleEntity());

        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.codigo").value("P-001"))
                .andExpect(jsonPath("$.nombre").value("Producto de prueba"))
                .andExpect(jsonPath("$.descripcion").value("Descripción de prueba"))
                .andExpect(jsonPath("$.unidadMedida").value("UN"))
                .andExpect(jsonPath("$.precio").value(1500.0))
                .andExpect(jsonPath("$.afectaIva").value(true))
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).create(any(ProductoRequest.class));
    }

    @Test
    void create_pasaLosDatosDelBodyAlServicio() throws Exception {
        when(service.create(any(ProductoRequest.class))).thenReturn(sampleEntity());

        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());

        ArgumentCaptor<ProductoRequest> captor = ArgumentCaptor.forClass(ProductoRequest.class);
        verify(service).create(captor.capture());

        ProductoRequest enviado = captor.getValue();
        assertThat(enviado.getCodigo()).isEqualTo("P-001");
        assertThat(enviado.getNombre()).isEqualTo("Producto de prueba");
        assertThat(enviado.getPrecio()).isEqualTo(1500.0);
        assertThat(enviado.getAfectaIva()).isTrue();
        assertThat(enviado.getActivo()).isTrue();
    }

    @Test
    void create_cuandoServicioLanzaReglaNegocio_devuelve400() throws Exception {
        when(service.create(any(ProductoRequest.class)))
                .thenThrow(new ReglaNegocioException("El código de producto ya existe"));

        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El código de producto ya existe"));
    }

    @Test
    void create_cuandoServicioLanzaRuntime_devuelve500() throws Exception {
        when(service.create(any(ProductoRequest.class)))
                .thenThrow(new RuntimeException("Fallo inesperado"));

        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("Fallo inesperado"));
    }

    // ---------------------------------------------------------------------
    // GET /productos -> findAll
    // ---------------------------------------------------------------------

    @Test
    void findAll_devuelve200YListaDeProductos() throws Exception {
        ProductoEntity otro = sampleEntity();
        otro.setId(8L);
        otro.setCodigo("P-002");
        otro.setNombre("Otro producto");

        when(service.findAll()).thenReturn(List.of(sampleEntity(), otro));

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].codigo").value("P-001"))
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[1].codigo").value("P-002"))
                .andExpect(jsonPath("$[1].nombre").value("Otro producto"));

        verify(service).findAll();
    }

    @Test
    void findAll_cuandoNoHayProductos_devuelveListaVacia() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).findAll();
    }

    // ---------------------------------------------------------------------
    // GET /productos/{id} -> getById
    // ---------------------------------------------------------------------

    @Test
    void getById_devuelve200YProducto() throws Exception {
        when(service.findById(7L)).thenReturn(sampleEntity());

        mockMvc.perform(get("/productos/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.codigo").value("P-001"))
                .andExpect(jsonPath("$.nombre").value("Producto de prueba"))
                .andExpect(jsonPath("$.precio").value(1500.0));

        verify(service).findById(7L);
    }

    @Test
    void getById_cuandoNoExiste_devuelve404() throws Exception {
        when(service.findById(99L))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado"));

        mockMvc.perform(get("/productos/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"));

        verify(service).findById(99L);
    }

    // ---------------------------------------------------------------------
    // PUT /productos/{id} -> update
    // ---------------------------------------------------------------------

    @Test
    void update_devuelve200YProductoActualizado() throws Exception {
        ProductoEntity actualizado = sampleEntity();
        actualizado.setNombre("Nombre actualizado");
        actualizado.setPrecio(2000.0);

        when(service.update(eq(7L), any(ProductoRequest.class))).thenReturn(actualizado);

        ProductoRequest request = sampleRequest();
        request.setNombre("Nombre actualizado");
        request.setPrecio(2000.0);

        mockMvc.perform(put("/productos/{id}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Nombre actualizado"))
                .andExpect(jsonPath("$.precio").value(2000.0));

        verify(service).update(eq(7L), any(ProductoRequest.class));
    }

    @Test
    void update_cuandoNoExiste_devuelve404() throws Exception {
        when(service.update(eq(99L), any(ProductoRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado"));

        mockMvc.perform(put("/productos/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"));

        verify(service).update(eq(99L), any(ProductoRequest.class));
    }

    @Test
    void update_cuandoServicioLanzaReglaNegocio_devuelve400() throws Exception {
        when(service.update(eq(7L), any(ProductoRequest.class)))
                .thenThrow(new ReglaNegocioException("Código duplicado"));

        mockMvc.perform(put("/productos/{id}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("Código duplicado"));
    }

    // ---------------------------------------------------------------------
    // DELETE /productos/{id} -> delete
    // ---------------------------------------------------------------------

    @Test
    void delete_devuelve204() throws Exception {
        doNothing().when(service).delete(7L);

        mockMvc.perform(delete("/productos/{id}", 7L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(7L);
    }

    @Test
    void delete_cuandoNoExiste_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Producto no encontrado"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/productos/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"));

        verify(service).delete(99L);
    }

    @Test
    void delete_cuandoEstaEnUso_devuelve400() throws Exception {
        doThrow(new ReglaNegocioException("El producto está en uso y no puede eliminarse"))
                .when(service).delete(7L);

        mockMvc.perform(delete("/productos/{id}", 7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El producto está en uso y no puede eliminarse"));

        verify(service).delete(7L);
    }
}
