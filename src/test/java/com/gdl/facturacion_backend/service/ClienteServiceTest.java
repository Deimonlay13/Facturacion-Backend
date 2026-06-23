package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.client.SreClient;
import com.gdl.facturacion_backend.dto.ClienteCreateRequest;
import com.gdl.facturacion_backend.dto.SreCompanyResponse;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.exception.ClienteDuplicadoException;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.exception.RutInvalidoException;
import com.gdl.facturacion_backend.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final Long EMPRESA_ID = 1L;

    // RUTs válidos (DV calculado con el mismo algoritmo de RutUtils).
    private static final String RUT_VALIDO = "12345678-5";
    private static final String RUT_VALIDO_2 = "76086428-5";
    // DV incorrecto a propósito.
    private static final String RUT_INVALIDO = "12345678-9";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private SreClient sreClient;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        // El servicio resuelve la empresa vía tenantService.getEmpresaId().
        // lenient porque algunos tests (ej. RUT inválido) cortan antes de usarlo.
        lenient().when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
    }

    private ClienteCreateRequest buildRequest(String rut) {
        ClienteCreateRequest request = new ClienteCreateRequest();
        request.setRut(rut);
        request.setRazonSocial("Empresa de Prueba SpA");
        request.setNombreFantasia("Prueba");
        request.setGiro("Servicios");
        request.setDireccion("Av. Siempre Viva 742");
        request.setCiudad("Santiago");
        request.setComuna("Providencia");
        request.setRegion("Metropolitana");
        request.setPais("Chile");
        request.setTelefono("+56912345678");
        request.setEmail("contacto@prueba.cl");
        return request;
    }

    private ClienteEntity buildEntity(Long id, String rut) {
        ClienteEntity entity = new ClienteEntity();
        entity.setId(id);
        entity.setRut(rut);
        entity.setRazonSocial("Empresa Existente SpA");
        entity.setActivo(true);
        return entity;
    }

    // ---------------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------------

    @Test
    void crear_conDatosValidos_persisteYMapeaCampos() {
        ClienteCreateRequest request = buildRequest(RUT_VALIDO);
        when(clienteRepository.existsByRutAndEmpresaId(RUT_VALIDO, EMPRESA_ID)).thenReturn(false);
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClienteEntity resultado = clienteService.crear(request);

        ArgumentCaptor<ClienteEntity> captor = ArgumentCaptor.forClass(ClienteEntity.class);
        verify(clienteRepository).save(captor.capture());
        ClienteEntity guardado = captor.getValue();

        assertThat(resultado).isSameAs(guardado);
        assertThat(guardado.getRut()).isEqualTo(RUT_VALIDO);
        assertThat(guardado.getRazonSocial()).isEqualTo("Empresa de Prueba SpA");
        assertThat(guardado.getNombreFantasia()).isEqualTo("Prueba");
        assertThat(guardado.getGiro()).isEqualTo("Servicios");
        assertThat(guardado.getDireccion()).isEqualTo("Av. Siempre Viva 742");
        assertThat(guardado.getCiudad()).isEqualTo("Santiago");
        assertThat(guardado.getComuna()).isEqualTo("Providencia");
        assertThat(guardado.getRegion()).isEqualTo("Metropolitana");
        assertThat(guardado.getPais()).isEqualTo("Chile");
        assertThat(guardado.getTelefono()).isEqualTo("+56912345678");
        assertThat(guardado.getEmail()).isEqualTo("contacto@prueba.cl");
        // save() de la base asigna la empresa del tenant.
        assertThat(guardado.getEmpresa()).isNotNull();
        assertThat(guardado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void crear_limpiaRutAntesDeValidarYConsultarDuplicado() {
        // RUT con puntos y minúscula: debe limpiarse a "12345678-5".
        ClienteCreateRequest request = buildRequest("12.345.678-5");
        when(clienteRepository.existsByRutAndEmpresaId(RUT_VALIDO, EMPRESA_ID)).thenReturn(false);
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClienteEntity resultado = clienteService.crear(request);

        assertThat(resultado.getRut()).isEqualTo(RUT_VALIDO);
        verify(clienteRepository).existsByRutAndEmpresaId(RUT_VALIDO, EMPRESA_ID);
    }

    @Test
    void crear_conRutInvalido_lanzaRutInvalidoException() {
        ClienteCreateRequest request = buildRequest(RUT_INVALIDO);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining(RUT_INVALIDO);

        verify(clienteRepository, never()).existsByRutAndEmpresaId(any(), any());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crear_conRutDuplicado_lanzaClienteDuplicadoException() {
        ClienteCreateRequest request = buildRequest(RUT_VALIDO);
        when(clienteRepository.existsByRutAndEmpresaId(RUT_VALIDO, EMPRESA_ID)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(ClienteDuplicadoException.class)
                .hasMessageContaining(RUT_VALIDO);

        verify(clienteRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // actualizar
    // ---------------------------------------------------------------------

    @Test
    void actualizar_conDatosValidosYMismoRut_actualizaYGuarda() {
        Long id = 10L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        ClienteCreateRequest request = buildRequest(RUT_VALIDO);

        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClienteEntity resultado = clienteService.actualizar(id, request);

        assertThat(resultado).isSameAs(existente);
        assertThat(resultado.getRazonSocial()).isEqualTo("Empresa de Prueba SpA");
        assertThat(resultado.getEmail()).isEqualTo("contacto@prueba.cl");
        // Mismo RUT -> no se consulta duplicado.
        verify(clienteRepository, never()).existsByRutAndEmpresaId(any(), any());
        verify(clienteRepository).save(existente);
    }

    @Test
    void actualizar_conRutNuevoNoDuplicado_actualizaYGuarda() {
        Long id = 10L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        ClienteCreateRequest request = buildRequest(RUT_VALIDO_2);

        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByRutAndEmpresaId(RUT_VALIDO_2, EMPRESA_ID)).thenReturn(false);
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClienteEntity resultado = clienteService.actualizar(id, request);

        assertThat(resultado.getRut()).isEqualTo(RUT_VALIDO_2);
        verify(clienteRepository).existsByRutAndEmpresaId(RUT_VALIDO_2, EMPRESA_ID);
        verify(clienteRepository).save(existente);
    }

    @Test
    void actualizar_clienteNoEncontrado_lanzaRecursoNoEncontradoException() {
        Long id = 99L;
        ClienteCreateRequest request = buildRequest(RUT_VALIDO);
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(id, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cliente no encontrado con id: " + id);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void actualizar_conRutInvalido_lanzaRutInvalidoException() {
        Long id = 10L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        ClienteCreateRequest request = buildRequest(RUT_INVALIDO);
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> clienteService.actualizar(id, request))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining(RUT_INVALIDO);

        verify(clienteRepository, never()).existsByRutAndEmpresaId(any(), any());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void actualizar_conRutNuevoDuplicado_lanzaClienteDuplicadoException() {
        Long id = 10L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        ClienteCreateRequest request = buildRequest(RUT_VALIDO_2);

        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByRutAndEmpresaId(RUT_VALIDO_2, EMPRESA_ID)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.actualizar(id, request))
                .isInstanceOf(ClienteDuplicadoException.class)
                .hasMessageContaining(RUT_VALIDO_2);

        verify(clienteRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // listarActivos
    // ---------------------------------------------------------------------

    @Test
    void listarActivos_devuelveSoloClientesActivos() {
        ClienteEntity activo1 = buildEntity(1L, RUT_VALIDO);
        activo1.setActivo(true);
        ClienteEntity inactivo = buildEntity(2L, RUT_VALIDO_2);
        inactivo.setActivo(false);
        ClienteEntity activo2 = buildEntity(3L, "76086428-5");
        activo2.setActivo(true);

        when(clienteRepository.findAllByEmpresaId(EMPRESA_ID))
                .thenReturn(List.of(activo1, inactivo, activo2));

        List<ClienteEntity> resultado = clienteService.listarActivos();

        assertThat(resultado).containsExactly(activo1, activo2);
    }

    @Test
    void listarActivos_ignoraClientesConActivoNull() {
        ClienteEntity activo = buildEntity(1L, RUT_VALIDO);
        activo.setActivo(true);
        ClienteEntity nulo = buildEntity(2L, RUT_VALIDO_2);
        nulo.setActivo(null);

        when(clienteRepository.findAllByEmpresaId(EMPRESA_ID))
                .thenReturn(List.of(activo, nulo));

        List<ClienteEntity> resultado = clienteService.listarActivos();

        assertThat(resultado).containsExactly(activo);
    }

    @Test
    void listarActivos_sinClientes_devuelveListaVacia() {
        when(clienteRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        assertThat(clienteService.listarActivos()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // obtenerPorId
    // ---------------------------------------------------------------------

    @Test
    void obtenerPorId_existente_devuelveCliente() {
        Long id = 5L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ClienteEntity resultado = clienteService.obtenerPorId(id);

        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void obtenerPorId_inexistente_lanzaRecursoNoEncontradoException() {
        Long id = 5L;
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorId(id))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cliente no encontrado con id: " + id);
    }

    // ---------------------------------------------------------------------
    // desactivar
    // ---------------------------------------------------------------------

    @Test
    void desactivar_existente_poneActivoFalseYGuarda() {
        Long id = 7L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        existente.setActivo(true);

        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.desactivar(id);

        assertThat(existente.getActivo()).isFalse();
        verify(clienteRepository).save(existente);
    }

    @Test
    void desactivar_inexistente_lanzaRecursoNoEncontradoException() {
        Long id = 7L;
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.desactivar(id))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(clienteRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // eliminar
    // ---------------------------------------------------------------------

    @Test
    void eliminar_existenteSinDependencias_borraYHaceFlush() {
        Long id = 8L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));

        clienteService.eliminar(id);

        verify(clienteRepository).delete(existente);
        verify(clienteRepository).flush();
    }

    @Test
    void eliminar_inexistente_lanzaRecursoNoEncontradoException() {
        Long id = 8L;
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.eliminar(id))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(clienteRepository, never()).delete(any());
        verify(clienteRepository, never()).flush();
    }

    @Test
    void eliminar_conDependencias_traduceDataIntegrityViolationAReglaNegocio() {
        Long id = 8L;
        ClienteEntity existente = buildEntity(id, RUT_VALIDO);
        when(clienteRepository.findByIdAndEmpresaId(id, EMPRESA_ID)).thenReturn(Optional.of(existente));
        // El flush dispara la violación de integridad referencial.
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(clienteRepository).flush();

        assertThatThrownBy(() -> clienteService.eliminar(id))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tiene documentos asociados");

        verify(clienteRepository).delete(existente);
        verify(clienteRepository).flush();
    }

    // ---------------------------------------------------------------------
    // consultarSre
    // ---------------------------------------------------------------------

    @Test
    void consultarSre_rutValidoEncontrado_devuelveRespuesta() {
        SreCompanyResponse esperado = new SreCompanyResponse();
        esperado.setRut(RUT_VALIDO);
        esperado.setRazonSocial("Empresa SRE SpA");
        when(sreClient.buscarPorRut(RUT_VALIDO)).thenReturn(Optional.of(esperado));

        Optional<SreCompanyResponse> resultado = clienteService.consultarSre(RUT_VALIDO);

        assertThat(resultado).containsSame(esperado);
        verify(sreClient).buscarPorRut(RUT_VALIDO);
    }

    @Test
    void consultarSre_limpiaRutAntesDeConsultar() {
        when(sreClient.buscarPorRut(RUT_VALIDO)).thenReturn(Optional.empty());

        Optional<SreCompanyResponse> resultado = clienteService.consultarSre("12.345.678-5");

        assertThat(resultado).isEmpty();
        verify(sreClient).buscarPorRut(RUT_VALIDO);
    }

    @Test
    void consultarSre_rutNoEncontrado_devuelveOptionalVacio() {
        when(sreClient.buscarPorRut(RUT_VALIDO)).thenReturn(Optional.empty());

        Optional<SreCompanyResponse> resultado = clienteService.consultarSre(RUT_VALIDO);

        assertThat(resultado).isEmpty();
    }

    @Test
    void consultarSre_rutInvalido_lanzaRutInvalidoExceptionYNoLlamaSre() {
        assertThatThrownBy(() -> clienteService.consultarSre(RUT_INVALIDO))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining(RUT_INVALIDO);

        verifyNoInteractions(sreClient);
    }

    // ---------------------------------------------------------------------
    // save (heredado de BaseTenantService) - guarda de propiedad del registro
    // ---------------------------------------------------------------------

    @Test
    void save_entidadConIdInexistente_lanzaRuntimeException() {
        // Vía actualizar no se puede llegar aquí porque findById ya valida;
        // se verifica directamente la guarda de save() heredada.
        ClienteEntity entidad = buildEntity(123L, RUT_VALIDO);
        when(clienteRepository.findByIdAndEmpresaId(123L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.save(entidad))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tienes permiso sobre este registro");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void save_entidadSinId_asignaEmpresaYGuarda() {
        ClienteEntity entidad = new ClienteEntity();
        entidad.setRut(RUT_VALIDO);
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClienteEntity resultado = clienteService.save(entidad);

        assertThat(resultado.getEmpresa()).isNotNull();
        assertThat(resultado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
        verify(clienteRepository, never()).findByIdAndEmpresaId(anyLong(), eq(EMPRESA_ID));
        verify(clienteRepository).save(entidad);
    }

    @Test
    void crear_resuelveEmpresaDesdeTenantService() {
        ClienteCreateRequest request = buildRequest(RUT_VALIDO);
        when(clienteRepository.existsByRutAndEmpresaId(RUT_VALIDO, EMPRESA_ID)).thenReturn(false);
        when(clienteRepository.save(any(ClienteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.crear(request);

        verify(tenantService, atLeastOnce()).getEmpresaId();
    }
}
