package com.gdl.facturacion_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.ProductoRepository;
import com.gdl.facturacion_backend.service.TenantService;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private ProductoRepository repository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private ProductoServiceImpl service;

    @Captor
    private ArgumentCaptor<ProductoEntity> productoCaptor;

    private ProductoRequest request;

    @BeforeEach
    void setUp() {
        request = new ProductoRequest();
        request.setCodigo("P-001");
        request.setNombre("Producto de prueba");
        request.setDescripcion("Descripción del producto");
        request.setUnidadMedida("UN");
        request.setPrecio(1500.0);
        request.setAfectaIva(true);
        request.setActivo(true);
    }

    private ProductoEntity productoExistente(Long id, String codigo) {
        ProductoEntity producto = new ProductoEntity();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre("Existente");
        producto.setDescripcion("Desc existente");
        producto.setUnidadMedida("KG");
        producto.setPrecio(999.0);
        producto.setAfectaIva(false);
        producto.setActivo(true);
        return producto;
    }

    // ---------------------------------------------------------------------
    // create
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("create: camino feliz guarda el producto con datos del request y activo=true")
    void create_caminoFeliz() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(false);
        when(repository.save(any(ProductoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoEntity resultado = service.create(request);

        verify(repository).save(productoCaptor.capture());
        ProductoEntity guardado = productoCaptor.getValue();

        assertThat(guardado.getCodigo()).isEqualTo("P-001");
        assertThat(guardado.getNombre()).isEqualTo("Producto de prueba");
        assertThat(guardado.getDescripcion()).isEqualTo("Descripción del producto");
        assertThat(guardado.getUnidadMedida()).isEqualTo("UN");
        assertThat(guardado.getPrecio()).isEqualTo(1500.0);
        assertThat(guardado.getAfectaIva()).isTrue();
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getEmpresa()).isNotNull();
        assertThat(guardado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);

        assertThat(resultado).isSameAs(guardado);
        verify(repository).existsByCodigoAndEmpresaId("P-001", EMPRESA_ID);
    }

    @Test
    @DisplayName("create: lanza ReglaNegocioException cuando el código ya existe y no guarda")
    void create_codigoDuplicado() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El código ya existe");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create: lanza ReglaNegocioException cuando el precio es 0")
    void create_precioCero() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(false);
        request.setPrecio(0.0);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El precio debe ser mayor a 0");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create: lanza ReglaNegocioException cuando el precio es negativo")
    void create_precioNegativo() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(false);
        request.setPrecio(-10.0);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El precio debe ser mayor a 0");

        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // update
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("update: mismo código no valida duplicado y actualiza todos los campos")
    void update_mismoCodigo() {
        ProductoEntity existente = productoExistente(5L, "P-001");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any(ProductoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setNombre("Actualizado");
        request.setActivo(false);
        request.setAfectaIva(false);

        ProductoEntity resultado = service.update(5L, request);

        assertThat(resultado.getNombre()).isEqualTo("Actualizado");
        assertThat(resultado.getCodigo()).isEqualTo("P-001");
        assertThat(resultado.getDescripcion()).isEqualTo("Descripción del producto");
        assertThat(resultado.getUnidadMedida()).isEqualTo("UN");
        assertThat(resultado.getPrecio()).isEqualTo(1500.0);
        assertThat(resultado.getAfectaIva()).isFalse();
        assertThat(resultado.getActivo()).isFalse();

        // mismo código => nunca se valida duplicado
        verify(repository, never()).existsByCodigoAndEmpresaId(anyString(), anyLong());
        verify(repository).save(existente);
    }

    @Test
    @DisplayName("update: código cambiado y no duplicado actualiza correctamente")
    void update_codigoCambiadoNoDuplicado() {
        ProductoEntity existente = productoExistente(5L, "P-OLD");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(false);
        when(repository.save(any(ProductoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoEntity resultado = service.update(5L, request);

        assertThat(resultado.getCodigo()).isEqualTo("P-001");
        verify(repository).existsByCodigoAndEmpresaId("P-001", EMPRESA_ID);
        verify(repository).save(existente);
    }

    @Test
    @DisplayName("update: código cambiado y duplicado lanza ReglaNegocioException y no guarda")
    void update_codigoCambiadoDuplicado() {
        ProductoEntity existente = productoExistente(5L, "P-OLD");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El código ya existe");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update: precio <= 0 lanza ReglaNegocioException (mismo código)")
    void update_precioInvalido() {
        ProductoEntity existente = productoExistente(5L, "P-001");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        request.setPrecio(0.0);

        assertThatThrownBy(() -> service.update(5L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El precio debe ser mayor a 0");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update: producto inexistente lanza RecursoNoEncontradoException")
    void update_productoInexistente() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Producto no encontrado");

        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // findById
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("findById: devuelve el producto cuando existe")
    void findById_existe() {
        ProductoEntity existente = productoExistente(7L, "P-007");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ProductoEntity resultado = service.findById(7L);

        assertThat(resultado).isSameAs(existente);
        verify(repository).findByIdAndEmpresaId(7L, EMPRESA_ID);
    }

    @Test
    @DisplayName("findById: lanza RecursoNoEncontradoException cuando no existe")
    void findById_noExiste() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Producto no encontrado");
    }

    // ---------------------------------------------------------------------
    // findAll
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("findAll: devuelve la lista del repositorio para la empresa actual")
    void findAll_conResultados() {
        List<ProductoEntity> lista = List.of(productoExistente(1L, "A"), productoExistente(2L, "B"));
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(lista);

        List<ProductoEntity> resultado = service.findAll();

        assertThat(resultado).hasSize(2).containsExactlyElementsOf(lista);
        verify(repository).findAllByEmpresaId(EMPRESA_ID);
    }

    @Test
    @DisplayName("findAll: devuelve lista vacía cuando no hay productos")
    void findAll_vacio() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        List<ProductoEntity> resultado = service.findAll();

        assertThat(resultado).isEmpty();
    }

    // ---------------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("delete: elimina y hace flush cuando el producto existe")
    void delete_caminoFeliz() {
        ProductoEntity existente = productoExistente(10L, "P-010");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        service.delete(10L);

        verify(repository).delete(existente);
        verify(repository).flush();
    }

    @Test
    @DisplayName("delete: producto inexistente lanza RecursoNoEncontradoException y no borra")
    void delete_productoInexistente() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Producto no encontrado");

        verify(repository, never()).delete(any());
        verify(repository, never()).flush();
    }

    @Test
    @DisplayName("delete: traduce DataIntegrityViolationException a ReglaNegocioException")
    void delete_violacionIntegridad() {
        ProductoEntity existente = productoExistente(10L, "P-010");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(repository).flush();

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar el producto: está en uso en documentos.");

        verify(repository).delete(existente);
        verify(repository).flush();
    }

    @Test
    @DisplayName("delete: violación de integridad lanzada por delete también se traduce")
    void delete_violacionEnDelete() {
        ProductoEntity existente = productoExistente(10L, "P-010");
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(repository).delete(existente);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar el producto: está en uso en documentos.");

        verify(repository).delete(existente);
        verify(repository, never()).flush();
    }

    // ---------------------------------------------------------------------
    // tenant / contexto
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("findAll: usa el empresaId provisto por TenantService")
    void usaEmpresaIdDelTenantService() {
        when(tenantService.getEmpresaId()).thenReturn(42L);
        when(repository.findAllByEmpresaId(42L)).thenReturn(List.of());

        service.findAll();

        verify(tenantService).getEmpresaId();
        verify(repository).findAllByEmpresaId(42L);
        verify(repository, never()).findAllByEmpresaId(eq(EMPRESA_ID));
    }

    @Test
    @DisplayName("create con código duplicado: no toca save ni flush (solo valida)")
    void create_duplicado_noEfectosSecundarios() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.existsByCodigoAndEmpresaId("P-001", EMPRESA_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ReglaNegocioException.class);

        verify(repository, times(1)).existsByCodigoAndEmpresaId("P-001", EMPRESA_ID);
        verify(repository, never()).save(any());
        verify(repository, never()).flush();
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("update inexistente: no interactúa con validación de código ni save")
    void update_inexistente_sinValidacionCodigo() {
        when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(repository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(repository, never()).existsByCodigoAndEmpresaId(anyString(), anyLong());
        verify(repository, never()).save(any());
    }
}
