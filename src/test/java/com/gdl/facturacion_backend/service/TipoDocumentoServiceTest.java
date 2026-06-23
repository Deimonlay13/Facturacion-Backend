package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.tipoDocumento.TipoDocumentoResponseDto;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.repository.TipoDocumentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipoDocumentoService - test unitario")
class TipoDocumentoServiceTest {

    @Mock
    private TipoDocumentoRepository repository;

    @InjectMocks
    private TipoDocumentoService service;

    private TipoDocumentoEntity buildEntity(Long id, Integer codigoSii, String descripcion) {
        TipoDocumentoEntity entity = new TipoDocumentoEntity();
        entity.setId(id);
        entity.setCodigoSii(codigoSii);
        entity.setDescripcion(descripcion);
        return entity;
    }

    // ---------- findAll ----------

    @Test
    @DisplayName("findAll mapea todas las entidades a DTO conservando los datos")
    void findAll_devuelveListaMapeada() {
        TipoDocumentoEntity factura = buildEntity(1L, 33, "Factura Electronica");
        TipoDocumentoEntity boleta = buildEntity(2L, 39, "Boleta Electronica");
        when(repository.findAll()).thenReturn(List.of(factura, boleta));

        List<TipoDocumentoResponseDto> resultado = service.findAll();

        assertThat(resultado).hasSize(2);

        TipoDocumentoResponseDto primero = resultado.get(0);
        assertThat(primero.getId()).isEqualTo(1L);
        assertThat(primero.getCodigoSii()).isEqualTo(33);
        assertThat(primero.getDescripcion()).isEqualTo("Factura Electronica");

        TipoDocumentoResponseDto segundo = resultado.get(1);
        assertThat(segundo.getId()).isEqualTo(2L);
        assertThat(segundo.getCodigoSii()).isEqualTo(39);
        assertThat(segundo.getDescripcion()).isEqualTo("Boleta Electronica");

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("findAll devuelve lista vacia cuando el repositorio no tiene registros")
    void findAll_listaVacia() {
        when(repository.findAll()).thenReturn(List.of());

        List<TipoDocumentoResponseDto> resultado = service.findAll();

        assertThat(resultado).isNotNull().isEmpty();
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("findAll mapea un unico elemento con descripcion nula sin fallar")
    void findAll_unElementoConDescripcionNula() {
        TipoDocumentoEntity entity = buildEntity(5L, 52, null);
        when(repository.findAll()).thenReturn(List.of(entity));

        List<TipoDocumentoResponseDto> resultado = service.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(5L);
        assertThat(resultado.get(0).getCodigoSii()).isEqualTo(52);
        assertThat(resultado.get(0).getDescripcion()).isNull();
        verify(repository).findAll();
    }

    // ---------- findByCodigoSii ----------

    @Test
    @DisplayName("findByCodigoSii devuelve la entidad cuando existe")
    void findByCodigoSii_existente() {
        TipoDocumentoEntity entity = buildEntity(1L, 33, "Factura Electronica");
        when(repository.findByCodigoSii(33)).thenReturn(Optional.of(entity));

        TipoDocumentoEntity resultado = service.findByCodigoSii(33);

        assertThat(resultado).isSameAs(entity);
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCodigoSii()).isEqualTo(33);
        assertThat(resultado.getDescripcion()).isEqualTo("Factura Electronica");

        verify(repository).findByCodigoSii(33);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("findByCodigoSii lanza RecursoNoEncontradoException cuando no existe")
    void findByCodigoSii_noExistente() {
        when(repository.findByCodigoSii(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCodigoSii(999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Tipo de documento no existe: 999");

        verify(repository).findByCodigoSii(999);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("findByCodigoSii con codigo nulo delega en el repositorio y lanza excepcion si no existe")
    void findByCodigoSii_codigoNulo() {
        when(repository.findByCodigoSii(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCodigoSii(null))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Tipo de documento no existe: null");

        verify(repository).findByCodigoSii(null);
        verify(repository, never()).findAll();
    }
}
