package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.dto.sii.EnvioSiiResponse;
import com.gdl.facturacion_backend.dto.sii.EstadoSiiResponse;
import com.gdl.facturacion_backend.dto.sii.HistorialSiiAdminResponse;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EnvioSiiEntity;
import com.gdl.facturacion_backend.entity.HistorialEstadoSiiEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.EstadoEnvioSii;
import com.gdl.facturacion_backend.enums.EstadoSii;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.repository.EnvioSiiRepository;
import com.gdl.facturacion_backend.repository.HistorialEstadoSiiRepository;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario de {@link EnvioSiiService}, la simulación del diálogo con el SII.
 *
 * <p>Colabora con {@link DocumentoTributarioService} (cargar el documento),
 * {@link DocumentoExportService} (generar el XML/DTE) y tres repositorios
 * ({@link DocumentoTributarioRepository}, {@link EnvioSiiRepository},
 * {@link HistorialEstadoSiiRepository}) que aquí se mockean.
 *
 * <p>Se cubren los tres pasos del flujo:
 * <ul>
 *   <li>enviar: camino feliz, rechazo si no está EMITIDO, rechazo si ya está ACEPTADO
 *       y reintento que incrementa el contador de intentos.</li>
 *   <li>consultarEstado: aceptación, rechazo con motivo, error si no se envió y
 *       no-op si ya estaba resuelto.</li>
 *   <li>obtenerEstado: lectura del estado + historial.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnvioSiiService - simulación de envío al SII")
class EnvioSiiServiceTest {

    private static final Long DOC_ID = 100L;

    @Mock
    private DocumentoTributarioService documentoService;

    @Mock
    private DocumentoTributarioRepository documentoRepository;

    @Mock
    private EnvioSiiRepository envioRepository;

    @Mock
    private HistorialEstadoSiiRepository historialRepository;

    @Mock
    private DocumentoExportService exportService;

    @InjectMocks
    private EnvioSiiService service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void autenticarComoSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("root", null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
    }

    // ---------------------------------------------------------------- fixtures

    /** Documento válido para aceptación: folio, ambos RUT y monto total > 0. */
    private DocumentoTributarioEntity documentoValido() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setId(DOC_ID);
        doc.setFolio(55);
        doc.setEstado(EstadoDocumento.EMITIDO);
        doc.setRutEmisor("76086428-5");
        doc.setRut("11111111-1");
        doc.setMontoTotal(new BigDecimal("11900"));
        return doc;
    }

    private EnvioSiiEntity envio(EstadoEnvioSii estado, int intentos) {
        EnvioSiiEntity e = new EnvioSiiEntity();
        e.setId(9L);
        e.setTrackId("SIM-55-abc12345");
        e.setFechaEnvio(LocalDateTime.now());
        e.setEstado(estado);
        e.setIntentos(intentos);
        return e;
    }

    private void devuelveArgumentoAlGuardarEnvio() {
        when(envioRepository.save(any(EnvioSiiEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void sinHistorialPrevio() {
        when(historialRepository.findByDocumentoIdOrderByFechaEstadoAsc(anyLong()))
                .thenReturn(List.of());
    }

    // ------------------------------------------------------------------ enviar

    @Nested
    @DisplayName("enviar")
    class Enviar {

        @Test
        void enviar_documentoEmitido_generaTrackIdYDejaEnviado() {
            DocumentoTributarioEntity doc = documentoValido();
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(exportService.generarXml(DOC_ID)).thenReturn(new byte[]{1, 2, 3, 4, 5});
            devuelveArgumentoAlGuardarEnvio();
            sinHistorialPrevio();

            EstadoSiiResponse resp = service.enviar(DOC_ID);

            assertThat(resp.getEstadoSii()).isEqualTo("ENVIADO");
            assertThat(resp.getTrackId()).startsWith("SIM-55-");
            assertThat(resp.getEnvio()).isNotNull();
            assertThat(resp.getEnvio().getEstado()).isEqualTo("ENVIADO");
            assertThat(resp.getEnvio().getIntentos()).isEqualTo(1);

            // Efectos sobre el documento
            assertThat(doc.getEstadoSii()).isEqualTo(EstadoDocumentoSii.ENVIADO);
            assertThat(doc.getXmlFirmado()).isTrue();
            assertThat(doc.getEnvio()).isNotNull();

            // Se persiste envío, documento e historial (transición ENVIADO)
            verify(envioRepository).save(any(EnvioSiiEntity.class));
            verify(documentoRepository).save(doc);

            ArgumentCaptor<HistorialEstadoSiiEntity> captor =
                    ArgumentCaptor.forClass(HistorialEstadoSiiEntity.class);
            verify(historialRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSii.ENVIADO);
            assertThat(captor.getValue().getDocumento()).isSameAs(doc);
        }

        @Test
        void enviar_documentoNoEmitido_lanzaReglaNegocio() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEstado(EstadoDocumento.BORRADOR);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);

            assertThatThrownBy(() -> service.enviar(DOC_ID))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("EMITIDOS");

            verify(envioRepository, never()).save(any());
            verify(documentoRepository, never()).save(any());
            verify(historialRepository, never()).save(any());
        }

        @Test
        void enviar_documentoYaAceptado_lanzaReglaNegocio() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEstadoSii(EstadoDocumentoSii.ACEPTADO);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);

            assertThatThrownBy(() -> service.enviar(DOC_ID))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("ya fue aceptado");

            verify(envioRepository, never()).save(any());
        }

        @Test
        void enviar_reintentoTrasRechazo_incrementaIntentos() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEstadoSii(EstadoDocumentoSii.RECHAZADO);
            doc.setEnvio(envio(EstadoEnvioSii.RECHAZADO, 1)); // intento previo
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(exportService.generarXml(DOC_ID)).thenReturn(new byte[]{9});
            devuelveArgumentoAlGuardarEnvio();
            sinHistorialPrevio();

            EstadoSiiResponse resp = service.enviar(DOC_ID);

            assertThat(resp.getEnvio().getIntentos()).isEqualTo(2);
            assertThat(resp.getEstadoSii()).isEqualTo("ENVIADO");
        }
    }

    // --------------------------------------------------------- consultarEstado

    @Nested
    @DisplayName("consultarEstado")
    class ConsultarEstado {

        @Test
        void consultar_documentoValido_loAcepta() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEstadoSii(EstadoDocumentoSii.ENVIADO);
            EnvioSiiEntity envio = envio(EstadoEnvioSii.ENVIADO, 1);
            doc.setEnvio(envio);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            sinHistorialPrevio();

            EstadoSiiResponse resp = service.consultarEstado(DOC_ID);

            assertThat(resp.getEstadoSii()).isEqualTo("ACEPTADO");
            assertThat(doc.getEstadoSii()).isEqualTo(EstadoDocumentoSii.ACEPTADO);
            assertThat(envio.getEstado()).isEqualTo(EstadoEnvioSii.ACEPTADO);
            verify(envioRepository).save(envio);
            verify(documentoRepository).save(doc);

            ArgumentCaptor<HistorialEstadoSiiEntity> captor =
                    ArgumentCaptor.forClass(HistorialEstadoSiiEntity.class);
            verify(historialRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSii.ACEPTADO);
        }

        @Test
        void consultar_montoInvalido_loRechazaConMotivo() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEstadoSii(EstadoDocumentoSii.ENVIADO);
            doc.setMontoTotal(BigDecimal.ZERO); // reparo
            EnvioSiiEntity envio = envio(EstadoEnvioSii.ENVIADO, 1);
            doc.setEnvio(envio);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            sinHistorialPrevio();

            EstadoSiiResponse resp = service.consultarEstado(DOC_ID);

            assertThat(resp.getEstadoSii()).isEqualTo("RECHAZADO");
            assertThat(doc.getEstadoSii()).isEqualTo(EstadoDocumentoSii.RECHAZADO);
            assertThat(envio.getEstado()).isEqualTo(EstadoEnvioSii.RECHAZADO);
            assertThat(envio.getRespuesta()).contains("monto total inválido");

            ArgumentCaptor<HistorialEstadoSiiEntity> captor =
                    ArgumentCaptor.forClass(HistorialEstadoSiiEntity.class);
            verify(historialRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSii.RECHAZADO);
        }

        @Test
        void consultar_documentoNoEnviado_lanzaReglaNegocio() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEnvio(null); // nunca se envió
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);

            assertThatThrownBy(() -> service.consultarEstado(DOC_ID))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no ha sido enviado");

            verify(envioRepository, never()).save(any());
            verify(historialRepository, never()).save(any());
        }

        @Test
        void consultar_yaResuelto_devuelveEstadoSinCambios() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setEstadoSii(EstadoDocumentoSii.ACEPTADO);
            doc.setEnvio(envio(EstadoEnvioSii.ACEPTADO, 1));
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            sinHistorialPrevio();

            EstadoSiiResponse resp = service.consultarEstado(DOC_ID);

            assertThat(resp.getEstadoSii()).isEqualTo("ACEPTADO");
            verify(envioRepository, never()).save(any());
            verify(documentoRepository, never()).save(any());
            verify(historialRepository, never()).save(any());
        }
    }

    // ----------------------------------------------------------- obtenerEstado

    @Test
    void obtenerEstado_devuelveEstadoActualEHistorial() {
        DocumentoTributarioEntity doc = documentoValido();
        doc.setEstadoSii(EstadoDocumentoSii.ENVIADO);
        doc.setEnvio(envio(EstadoEnvioSii.ENVIADO, 1));
        when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);

        HistorialEstadoSiiEntity h = new HistorialEstadoSiiEntity();
        h.setEstado(EstadoSii.ENVIADO);
        h.setFechaEstado(LocalDateTime.now());
        h.setMensaje("Envío simulado al SII.");
        when(historialRepository.findByDocumentoIdOrderByFechaEstadoAsc(DOC_ID))
                .thenReturn(List.of(h));

        EstadoSiiResponse resp = service.obtenerEstado(DOC_ID);

        assertThat(resp.getDocumentoId()).isEqualTo(DOC_ID);
        assertThat(resp.getFolio()).isEqualTo(55);
        assertThat(resp.getEstadoSii()).isEqualTo("ENVIADO");
        assertThat(resp.getTrackId()).isEqualTo("SIM-55-abc12345");
        assertThat(resp.getHistorial()).hasSize(1);
        assertThat(resp.getHistorial().get(0).getEstado()).isEqualTo("ENVIADO");
        assertThat(resp.getHistorial().get(0).getMensaje()).isEqualTo("Envío simulado al SII.");

        // Lectura pura: no persiste nada
        verify(envioRepository, never()).save(any());
        verify(documentoRepository, never()).save(any());
        verify(historialRepository, never()).save(any());
    }

    // ----------------------------------------------- listados de administración

    @Test
    void listarEnvios_comoSuperAdmin_devuelveTodosOrdenadosPorIdDesc() {
        autenticarComoSuperAdmin();
        EnvioSiiEntity e1 = envio(EstadoEnvioSii.ENVIADO, 1);
        e1.setId(1L);
        EnvioSiiEntity e2 = envio(EstadoEnvioSii.ACEPTADO, 1);
        e2.setId(2L);
        when(envioRepository.findAll()).thenReturn(List.of(e1, e2));

        List<EnvioSiiResponse> resp = service.listarEnvios();

        assertThat(resp).extracting(EnvioSiiResponse::getId).containsExactly(2L, 1L); // más nuevo primero
        verify(envioRepository).findAll();
        verify(envioRepository, never()).findAllByEmpresaId(any());
    }

    @Test
    void listarEnvios_comoUsuarioNormal_acotaPorEmpresa() {
        TenantContext.setEmpresaId(7L);
        EnvioSiiEntity e1 = envio(EstadoEnvioSii.ENVIADO, 1);
        e1.setId(5L);
        when(envioRepository.findAllByEmpresaId(7L)).thenReturn(List.of(e1));

        List<EnvioSiiResponse> resp = service.listarEnvios();

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getId()).isEqualTo(5L);
        verify(envioRepository).findAllByEmpresaId(7L);
        verify(envioRepository, never()).findAll();
    }

    @Test
    void listarHistorial_comoSuperAdmin_incluyeDocumentoYFolio() {
        autenticarComoSuperAdmin();
        DocumentoTributarioEntity doc = documentoValido(); // id=100, folio=55
        HistorialEstadoSiiEntity h = new HistorialEstadoSiiEntity();
        h.setId(3L);
        h.setDocumento(doc);
        h.setEstado(EstadoSii.ACEPTADO);
        h.setFechaEstado(LocalDateTime.now());
        h.setMensaje("El SII aceptó el documento (simulado).");
        when(historialRepository.findAll()).thenReturn(List.of(h));

        List<HistorialSiiAdminResponse> resp = service.listarHistorial();

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getDocumentoId()).isEqualTo(100L);
        assertThat(resp.get(0).getFolio()).isEqualTo(55);
        assertThat(resp.get(0).getEstado()).isEqualTo("ACEPTADO");
        verify(historialRepository).findAll();
        verify(historialRepository, never()).findAllByEmpresaId(any());
    }
}
