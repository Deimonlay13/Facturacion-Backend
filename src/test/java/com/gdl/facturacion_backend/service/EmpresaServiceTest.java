package com.gdl.facturacion_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.exception.RutInvalidoException;
import com.gdl.facturacion_backend.repository.EmpresaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaService - tests unitarios")
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository repository;

    @InjectMocks
    private EmpresaService service;

    // RUTs reales validados con RutUtils.calculateDv:
    // 12345678-5, 22222222-2, 11111111-1 son válidos.
    private static final String RUT_VALIDO = "12345678-5";
    private static final String RUT_VALIDO_LIMPIO = "12345678-5";
    private static final String RUT_VALIDO_2 = "22222222-2";
    private static final String RUT_INVALIDO = "12345678-9"; // DV incorrecto

    private EmpresaCreateRequest request;

    @BeforeEach
    void setUp() {
        request = buildRequest(RUT_VALIDO);
    }

    private EmpresaCreateRequest buildRequest(String rut) {
        EmpresaCreateRequest r = new EmpresaCreateRequest();
        r.setRutEmpresa(rut);
        r.setRazonSocial("Empresa de Prueba SpA");
        r.setNombreFantasia("Prueba");
        r.setGiro("Servicios");
        r.setDireccion("Av. Siempre Viva 742");
        r.setCiudad("Santiago");
        r.setComuna("Providencia");
        r.setPais("Chile");
        r.setTelefono("+56 9 1234 5678");
        r.setSitioWeb("https://prueba.cl");
        r.setEmailPrincipal("contacto@prueba.cl");
        r.setEmailContabilidad("conta@prueba.cl");
        r.setRutRepresentante("11111111-1");
        r.setNombreRepresentante("Juan Pérez");
        r.setTelefonoRepresentante("+56 9 8765 4321");
        return r;
    }

    private EmpresaEntity buildEntity(Long id, String rut) {
        EmpresaEntity e = new EmpresaEntity();
        e.setId(id);
        e.setRutEmpresa(rut);
        e.setRazonSocial("Antigua Razon Social");
        e.setActivo(true);
        return e;
    }

    // ---------------------------------------------------------------------
    // create()
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("crea la empresa cuando el RUT es válido y no existe")
        void create_ok() {
            when(repository.findByRutEmpresa(RUT_VALIDO_LIMPIO)).thenReturn(Optional.empty());
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> {
                EmpresaEntity saved = inv.getArgument(0);
                saved.setId(99L);
                return saved;
            });

            EmpresaEntity result = service.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getActivo()).isTrue();
            assertThat(result.getRutEmpresa()).isEqualTo(RUT_VALIDO_LIMPIO);
            assertThat(result.getRazonSocial()).isEqualTo("Empresa de Prueba SpA");
            assertThat(result.getNombreFantasia()).isEqualTo("Prueba");
            assertThat(result.getGiro()).isEqualTo("Servicios");
            assertThat(result.getDireccion()).isEqualTo("Av. Siempre Viva 742");
            assertThat(result.getCiudad()).isEqualTo("Santiago");
            assertThat(result.getComuna()).isEqualTo("Providencia");
            assertThat(result.getPais()).isEqualTo("Chile");
            assertThat(result.getTelefono()).isEqualTo("+56 9 1234 5678");
            assertThat(result.getSitioWeb()).isEqualTo("https://prueba.cl");
            assertThat(result.getEmailPrincipal()).isEqualTo("contacto@prueba.cl");
            assertThat(result.getEmailContabilidad()).isEqualTo("conta@prueba.cl");
            assertThat(result.getRutRepresentante()).isEqualTo("11111111-1");
            assertThat(result.getNombreRepresentante()).isEqualTo("Juan Pérez");
            assertThat(result.getTelefonoRepresentante()).isEqualTo("+56 9 8765 4321");

            verify(repository).findByRutEmpresa(RUT_VALIDO_LIMPIO);
            verify(repository).save(any(EmpresaEntity.class));
        }

        @Test
        @DisplayName("limpia el RUT (quita puntos, trim y mayúsculas) antes de persistir")
        void create_limpiaRut() {
            EmpresaCreateRequest req = buildRequest("  12.345.678-5  ");
            when(repository.findByRutEmpresa(RUT_VALIDO_LIMPIO)).thenReturn(Optional.empty());
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpresaEntity result = service.create(req);

            assertThat(result.getRutEmpresa()).isEqualTo(RUT_VALIDO_LIMPIO);

            ArgumentCaptor<EmpresaEntity> captor = ArgumentCaptor.forClass(EmpresaEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getRutEmpresa()).isEqualTo(RUT_VALIDO_LIMPIO);
            verify(repository).findByRutEmpresa(RUT_VALIDO_LIMPIO);
        }

        @Test
        @DisplayName("lanza ReglaNegocioException si ya existe una empresa con ese RUT")
        void create_rutDuplicado() {
            when(repository.findByRutEmpresa(RUT_VALIDO_LIMPIO))
                    .thenReturn(Optional.of(buildEntity(1L, RUT_VALIDO_LIMPIO)));

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(RUT_VALIDO_LIMPIO);

            verify(repository).findByRutEmpresa(RUT_VALIDO_LIMPIO);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza RutInvalidoException si el RUT tiene DV incorrecto")
        void create_rutInvalido() {
            request.setRutEmpresa(RUT_INVALIDO);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(RutInvalidoException.class)
                    .hasMessageContaining(RUT_INVALIDO);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("lanza RutInvalidoException si el RUT es null")
        void create_rutNull() {
            request.setRutEmpresa(null);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(RutInvalidoException.class);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("lanza RutInvalidoException si el RUT es blanco")
        void create_rutBlanco() {
            request.setRutEmpresa("   ");

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(RutInvalidoException.class);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("lanza RutInvalidoException si el RUT no tiene formato esperado")
        void create_rutSinFormato() {
            request.setRutEmpresa("ABCDEF");

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(RutInvalidoException.class);

            verifyNoInteractions(repository);
        }
    }

    // ---------------------------------------------------------------------
    // findAll()
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("devuelve todas las empresas")
        void findAll_ok() {
            List<EmpresaEntity> empresas = List.of(
                    buildEntity(1L, RUT_VALIDO_LIMPIO),
                    buildEntity(2L, RUT_VALIDO_2));
            when(repository.findAll()).thenReturn(empresas);

            List<EmpresaEntity> result = service.findAll();

            assertThat(result).hasSize(2).containsExactlyElementsOf(empresas);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("devuelve lista vacía cuando no hay empresas")
        void findAll_vacio() {
            when(repository.findAll()).thenReturn(List.of());

            List<EmpresaEntity> result = service.findAll();

            assertThat(result).isEmpty();
            verify(repository).findAll();
        }
    }

    // ---------------------------------------------------------------------
    // findById()
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("devuelve la empresa cuando existe")
        void findById_ok() {
            EmpresaEntity entity = buildEntity(5L, RUT_VALIDO_LIMPIO);
            when(repository.findById(5L)).thenReturn(Optional.of(entity));

            EmpresaEntity result = service.findById(5L);

            assertThat(result).isSameAs(entity);
            verify(repository).findById(5L);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException cuando no existe")
        void findById_noEncontrado() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");

            verify(repository).findById(404L);
        }
    }

    // ---------------------------------------------------------------------
    // update()
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("actualiza la empresa cuando el RUT no cambia (misma empresa)")
        void update_mismoRut() {
            EmpresaEntity existente = buildEntity(10L, RUT_VALIDO_LIMPIO);
            when(repository.findById(10L)).thenReturn(Optional.of(existente));
            when(repository.findByRutEmpresa(RUT_VALIDO_LIMPIO))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpresaEntity result = service.update(10L, request);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getRazonSocial()).isEqualTo("Empresa de Prueba SpA");
            assertThat(result.getRutEmpresa()).isEqualTo(RUT_VALIDO_LIMPIO);
            verify(repository).findById(10L);
            verify(repository).findByRutEmpresa(RUT_VALIDO_LIMPIO);
            verify(repository).save(existente);
        }

        @Test
        @DisplayName("actualiza la empresa cuando el RUT cambia a uno no usado")
        void update_rutNuevoLibre() {
            EmpresaEntity existente = buildEntity(10L, RUT_VALIDO_2);
            EmpresaCreateRequest req = buildRequest(RUT_VALIDO);
            when(repository.findById(10L)).thenReturn(Optional.of(existente));
            when(repository.findByRutEmpresa(RUT_VALIDO_LIMPIO)).thenReturn(Optional.empty());
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpresaEntity result = service.update(10L, req);

            assertThat(result.getRutEmpresa()).isEqualTo(RUT_VALIDO_LIMPIO);
            verify(repository).save(existente);
        }

        @Test
        @DisplayName("lanza ReglaNegocioException si el RUT pertenece a otra empresa")
        void update_rutDeOtraEmpresa() {
            EmpresaEntity existente = buildEntity(10L, RUT_VALIDO_2);
            EmpresaEntity otra = buildEntity(20L, RUT_VALIDO_LIMPIO);
            EmpresaCreateRequest req = buildRequest(RUT_VALIDO);
            when(repository.findById(10L)).thenReturn(Optional.of(existente));
            when(repository.findByRutEmpresa(RUT_VALIDO_LIMPIO)).thenReturn(Optional.of(otra));

            assertThatThrownBy(() -> service.update(10L, req))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(RUT_VALIDO_LIMPIO);

            verify(repository).findById(10L);
            verify(repository).findByRutEmpresa(RUT_VALIDO_LIMPIO);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si la empresa a actualizar no existe")
        void update_noEncontrada() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404L, request))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");

            verify(repository).findById(404L);
            verify(repository, never()).save(any());
            verify(repository, never()).findByRutEmpresa(any());
        }

        @Test
        @DisplayName("lanza RutInvalidoException si el RUT del request es inválido")
        void update_rutInvalido() {
            EmpresaEntity existente = buildEntity(10L, RUT_VALIDO_2);
            EmpresaCreateRequest req = buildRequest(RUT_INVALIDO);
            when(repository.findById(10L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(10L, req))
                    .isInstanceOf(RutInvalidoException.class)
                    .hasMessageContaining(RUT_INVALIDO);

            verify(repository).findById(10L);
            verify(repository, never()).findByRutEmpresa(any());
            verify(repository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------
    // cambiarEstado()
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("cambiarEstado()")
    class CambiarEstado {

        @Test
        @DisplayName("activa la empresa")
        void cambiarEstado_activar() {
            EmpresaEntity entity = buildEntity(7L, RUT_VALIDO_LIMPIO);
            entity.setActivo(false);
            when(repository.findById(7L)).thenReturn(Optional.of(entity));
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpresaEntity result = service.cambiarEstado(7L, true);

            assertThat(result.getActivo()).isTrue();
            verify(repository).findById(7L);
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("desactiva la empresa")
        void cambiarEstado_desactivar() {
            EmpresaEntity entity = buildEntity(7L, RUT_VALIDO_LIMPIO);
            entity.setActivo(true);
            when(repository.findById(7L)).thenReturn(Optional.of(entity));
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpresaEntity result = service.cambiarEstado(7L, false);

            assertThat(result.getActivo()).isFalse();
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si la empresa no existe")
        void cambiarEstado_noEncontrada() {
            when(repository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cambiarEstado(404L, true))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");

            verify(repository).findById(404L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("acepta estado null y lo asigna")
        void cambiarEstado_null() {
            EmpresaEntity entity = buildEntity(7L, RUT_VALIDO_LIMPIO);
            when(repository.findById(7L)).thenReturn(Optional.of(entity));
            when(repository.save(any(EmpresaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpresaEntity result = service.cambiarEstado(7L, null);

            assertThat(result.getActivo()).isNull();
            verify(repository, times(1)).save(entity);
        }
    }
}
