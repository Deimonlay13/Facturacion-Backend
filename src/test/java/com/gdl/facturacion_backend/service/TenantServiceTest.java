package com.gdl.facturacion_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gdl.facturacion_backend.context.TenantContext;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @InjectMocks
    private TenantService tenantService;

    @AfterEach
    void tearDown() {
        // Limpiamos el ThreadLocal para no contaminar otros tests.
        TenantContext.clear();
    }

    @Test
    @DisplayName("getEmpresaId devuelve el id cuando el contexto tiene una empresa")
    void getEmpresaId_devuelveIdCuandoHayEmpresaEnContexto() {
        TenantContext.setEmpresaId(1L);

        Long resultado = tenantService.getEmpresaId();

        assertThat(resultado).isEqualTo(1L);
    }

    @Test
    @DisplayName("getEmpresaId devuelve el id exacto seteado (otro valor)")
    void getEmpresaId_devuelveIdExactoSeteado() {
        TenantContext.setEmpresaId(99L);

        Long resultado = tenantService.getEmpresaId();

        assertThat(resultado).isEqualTo(99L);
    }

    @Test
    @DisplayName("getEmpresaId lanza RuntimeException cuando no hay empresa en el contexto")
    void getEmpresaId_lanzaExcepcionCuandoNoHayEmpresa() {
        // No seteamos nada en el contexto -> getEmpresaId() del contexto devuelve null.
        TenantContext.clear();

        assertThatThrownBy(() -> tenantService.getEmpresaId())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No se encontró empresa en el contexto del tenant");
    }

    @Test
    @DisplayName("getEmpresaId lanza RuntimeException cuando el contexto se setea explícitamente a null")
    void getEmpresaId_lanzaExcepcionCuandoEmpresaEsNull() {
        TenantContext.setEmpresaId(null);

        assertThatThrownBy(() -> tenantService.getEmpresaId())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No se encontró empresa en el contexto del tenant");
    }
}
