package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.RolRequest;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.RolRepository;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository roleRepository;

    @InjectMocks
    private RolService rolService;

    private RolRequest buildRequest(String nombre, String nombreMostrar, String descripcion) {
        RolRequest request = new RolRequest();
        request.setNombre(nombre);
        request.setNombreMostrar(nombreMostrar);
        request.setDescripcion(descripcion);
        return request;
    }

    // ---------- save ----------

    @Test
    void save_agregaPrefijoRoleYNormalizaAMayusculas_cuandoNoTienePrefijo() {
        RolRequest request = buildRequest("vendedor", "Vendedor", "Rol de vendedor");
        when(roleRepository.findByNombre("ROLE_VENDEDOR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(RolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RolEntity resultado = rolService.save(request);

        ArgumentCaptor<RolEntity> captor = ArgumentCaptor.forClass(RolEntity.class);
        verify(roleRepository).save(captor.capture());
        RolEntity guardado = captor.getValue();

        assertThat(guardado.getNombre()).isEqualTo("ROLE_VENDEDOR");
        assertThat(guardado.getNombreMostrar()).isEqualTo("Vendedor");
        assertThat(guardado.getDescripcion()).isEqualTo("Rol de vendedor");
        assertThat(guardado.getActivo()).isTrue();
        assertThat(resultado).isSameAs(guardado);
        verify(roleRepository).findByNombre("ROLE_VENDEDOR");
    }

    @Test
    void save_conservaPrefijoExistente_cuandoNombreYaEmpiezaConRole() {
        RolRequest request = buildRequest("role_admin", "Administrador", "Rol admin");
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(RolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        rolService.save(request);

        ArgumentCaptor<RolEntity> captor = ArgumentCaptor.forClass(RolEntity.class);
        verify(roleRepository).save(captor.capture());
        // No debe quedar doble prefijo (ROLE_ROLE_ADMIN)
        assertThat(captor.getValue().getNombre()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void save_lanzaRuntimeException_cuandoRolYaExiste() {
        RolRequest request = buildRequest("admin", "Administrador", "desc");
        when(roleRepository.findByNombre("ROLE_ADMIN"))
                .thenReturn(Optional.of(new RolEntity()));

        assertThatThrownBy(() -> rolService.save(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El rol ROLE_ADMIN ya existe");

        verify(roleRepository).findByNombre("ROLE_ADMIN");
        verify(roleRepository, never()).save(any());
    }

    // ---------- findByNombre ----------

    @Test
    void findByNombre_retornaRol_cuandoExiste() {
        RolEntity rol = new RolEntity();
        rol.setNombre("ROLE_ADMIN");
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(rol));

        RolEntity resultado = rolService.findByNombre("ROLE_ADMIN");

        assertThat(resultado).isSameAs(rol);
        verify(roleRepository).findByNombre("ROLE_ADMIN");
    }

    @Test
    void findByNombre_lanzaRuntimeException_cuandoNoExiste() {
        when(roleRepository.findByNombre("ROLE_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.findByNombre("ROLE_X"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El rol ROLE_X no existe en el sistema");
    }

    // ---------- findAll ----------

    @Test
    void findAll_retornaListaDeRoles() {
        RolEntity r1 = new RolEntity();
        RolEntity r2 = new RolEntity();
        when(roleRepository.findAll()).thenReturn(List.of(r1, r2));

        List<RolEntity> resultado = rolService.findAll();

        assertThat(resultado).containsExactly(r1, r2);
        verify(roleRepository).findAll();
    }

    @Test
    void findAll_retornaListaVacia_cuandoNoHayRoles() {
        when(roleRepository.findAll()).thenReturn(List.of());

        assertThat(rolService.findAll()).isEmpty();
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_borraRol_cuandoExisteYSePuedeEliminar() {
        RolEntity rol = new RolEntity();
        rol.setCanDelete(true);
        when(roleRepository.findById(5L)).thenReturn(Optional.of(rol));

        rolService.eliminar(5L);

        verify(roleRepository).findById(5L);
        verify(roleRepository).delete(rol);
        verify(roleRepository).flush();
    }

    @Test
    void eliminar_lanzaRecursoNoEncontrado_cuandoRolNoExiste() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Rol no encontrado con id: 99");

        verify(roleRepository, never()).delete(any());
        verify(roleRepository, never()).flush();
    }

    @Test
    void eliminar_lanzaReglaNegocio_cuandoCanDeleteEsFalse() {
        RolEntity rol = new RolEntity();
        rol.setCanDelete(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(rol));

        assertThatThrownBy(() -> rolService.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Este rol del sistema no se puede eliminar.");

        verify(roleRepository, never()).delete(any());
        verify(roleRepository, never()).flush();
    }

    @Test
    void eliminar_lanzaReglaNegocio_cuandoViolacionIntegridadAlEliminar() {
        RolEntity rol = new RolEntity();
        rol.setCanDelete(true);
        when(roleRepository.findById(7L)).thenReturn(Optional.of(rol));
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(roleRepository).flush();

        assertThatThrownBy(() -> rolService.eliminar(7L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar el rol: está asignado a uno o más usuarios.");

        verify(roleRepository).delete(rol);
        verify(roleRepository).flush();
    }

    @Test
    void eliminar_noLanzaError_cuandoCanDeleteEsNull() {
        // Boolean.FALSE.equals(null) == false, por lo que un canDelete nulo permite eliminar
        RolEntity rol = new RolEntity();
        rol.setCanDelete(null);
        when(roleRepository.findById(3L)).thenReturn(Optional.of(rol));

        rolService.eliminar(3L);

        verify(roleRepository).delete(rol);
        verify(roleRepository).flush();
    }
}
