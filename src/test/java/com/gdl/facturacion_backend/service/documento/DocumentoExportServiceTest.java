package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.Moneda;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.repository.EmpresaRepository;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import com.gdl.facturacion_backend.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test unitario de {@link DocumentoExportService}.
 *
 * <p>El servicio colabora con {@link DocumentoTributarioService} (obtener el documento),
 * {@link EmpresaRepository} + {@link TenantService} (resolver el emisor del tenant actual)
 * y, de forma estática, con {@code DocumentoPdfGenerator} para el PDF.
 *
 * <p>Se cubren:
 * <ul>
 *   <li>generarXml: camino feliz con datos vivos, snapshot, detalles, observaciones,
 *       valores nulos, receptor nulo y la rama de fallback de TipoDTE/moneda/estado.</li>
 *   <li>generarPdf: camino feliz y propagación de errores.</li>
 *   <li>emisor(): RecursoNoEncontradoException cuando la empresa no existe.</li>
 *   <li>Propagación de excepciones desde el servicio de documentos.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoExportService - generación de XML y PDF")
class DocumentoExportServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long DOC_ID = 100L;

    @Mock
    private DocumentoTributarioService documentoService;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private DocumentoExportService service;

    private EmpresaEntity emisor;

    @BeforeEach
    void setUp() {
        emisor = empresaCompleta();
    }

    // ============================================================ helpers de datos

    private EmpresaEntity empresaCompleta() {
        EmpresaEntity e = new EmpresaEntity();
        e.setId(EMPRESA_ID);
        e.setRutEmpresa("76123456-7");
        e.setRazonSocial("Empresa Emisora SpA");
        e.setNombreFantasia("Emisora");
        e.setGiro("Servicios informáticos");
        e.setDireccion("Av. Siempre Viva 742");
        e.setComuna("Providencia");
        e.setCiudad("Santiago");
        e.setTelefono("+56 2 2345 6789");
        e.setSitioWeb("www.emisora.cl");
        e.setEmailPrincipal("contacto@emisora.cl");
        e.setEmailContabilidad("conta@emisora.cl");
        return e;
    }

    private TipoDocumentoEntity tipoDocumento(int codigoSii, String descripcion) {
        TipoDocumentoEntity t = new TipoDocumentoEntity();
        t.setCodigoSii(codigoSii);
        t.setDescripcion(descripcion);
        return t;
    }

    private ClienteEntity clienteCompleto() {
        ClienteEntity c = new ClienteEntity();
        c.setId(50L);
        c.setRut("11111111-1");
        c.setRazonSocial("Cliente Receptor Ltda");
        c.setGiro("Comercio");
        c.setDireccion("Calle Falsa 123");
        c.setComuna("Las Condes");
        c.setCiudad("Santiago");
        c.setEmail("cliente@receptor.cl");
        return c;
    }

    private DetalleDocumentoEntity detalle(String codigoProducto, String descripcion,
                                           String cantidad, String precio, String subtotal) {
        DetalleDocumentoEntity d = new DetalleDocumentoEntity();
        if (codigoProducto != null) {
            ProductoEntity p = new ProductoEntity();
            p.setCodigo(codigoProducto);
            d.setProducto(p);
        }
        d.setDescripcionItem(descripcion);
        d.setCantidad(cantidad != null ? new BigDecimal(cantidad) : null);
        d.setPrecioUnitario(precio != null ? new BigDecimal(precio) : null);
        d.setUnidadMedida("UN");
        d.setSubtotal(subtotal != null ? new BigDecimal(subtotal) : null);
        return d;
    }

    /** Documento completo y consistente, en estado EMITIDO con snapshot lleno. */
    private DocumentoTributarioEntity documentoCompleto() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setId(DOC_ID);
        doc.setTipoDocumento(tipoDocumento(33, "Factura Electrónica"));
        doc.setCliente(clienteCompleto());
        doc.setFolio(123);
        doc.setFechaEmision(LocalDate.of(2026, 1, 15));
        doc.setFechaVencimiento(LocalDate.of(2026, 2, 15));
        doc.setEstado(EstadoDocumento.EMITIDO);
        doc.setMoneda(Moneda.CLP);
        doc.setTipoCambio(BigDecimal.ONE);

        // snapshot del receptor
        doc.setRut("22222222-2");
        doc.setRazonSocial("Receptor Snapshot SA");
        doc.setGiro("Snapshot giro");
        doc.setDireccion("Dir snapshot 1");
        doc.setComuna("Comuna snapshot");
        doc.setCiudad("Ciudad snapshot");
        doc.setCorreo("snapshot@receptor.cl");

        // snapshot del emisor
        doc.setRutEmisor("76999999-9");
        doc.setRazonSocialEmisor("Emisor Snapshot SpA");
        doc.setNombreFantasiaEmisor("EmisorSnap");
        doc.setGiroEmisor("Giro emisor snap");
        doc.setDireccionEmisor("Dir emisor snap");
        doc.setComunaEmisor("Comuna emisor snap");
        doc.setCiudadEmisor("Ciudad emisor snap");
        doc.setTelefonoEmisor("+56 9 1111 1111");
        doc.setEmailPrincipalEmisor("emisor@snap.cl");

        doc.setMontoNeto(new BigDecimal("10000"));
        doc.setMontoIva(new BigDecimal("1900"));
        doc.setMontoTotal(new BigDecimal("11900"));
        doc.setObservaciones("Observación de prueba");

        doc.setDetalles(new ArrayList<>(List.of(
                detalle("P001", "Producto uno", "2", "5000", "10000"))));
        doc.setReferencias(new ArrayList<>());
        return doc;
    }

    private String xml(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ============================================================ generarXml

    @Nested
    @DisplayName("generarXml")
    class GenerarXml {

        @Test
        @DisplayName("camino feliz: genera XML válido con encabezado, snapshot, totales y detalle")
        void generarXml_caminoFeliz() {
            DocumentoTributarioEntity doc = documentoCompleto();
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            byte[] bytes = service.generarXml(DOC_ID);

            assertThat(bytes).isNotEmpty();
            String xml = xml(bytes);
            assertThat(xml).contains("<DTE version=\"1.0\">");
            assertThat(xml).contains("<TipoDTE>33</TipoDTE>");
            assertThat(xml).contains("<Folio>123</Folio>");
            assertThat(xml).contains("<FchEmis>2026-01-15</FchEmis>");
            assertThat(xml).contains("<FchVenc>2026-02-15</FchVenc>");
            assertThat(xml).contains("<Estado>EMITIDO</Estado>");
            assertThat(xml).contains("<MntMoneda>CLP</MntMoneda>");
            // snapshot del emisor tiene prioridad sobre los datos vivos de la empresa
            assertThat(xml).contains("<RUTEmisor>76999999-9</RUTEmisor>");
            assertThat(xml).contains("<RznSoc>Emisor Snapshot SpA</RznSoc>");
            // snapshot del receptor tiene prioridad
            assertThat(xml).contains("<RUTRecep>22222222-2</RUTRecep>");
            assertThat(xml).contains("<RznSocRecep>Receptor Snapshot SA</RznSocRecep>");
            // totales
            assertThat(xml).contains("<MntNeto>10000</MntNeto>");
            assertThat(xml).contains("<IVA>1900</IVA>");
            assertThat(xml).contains("<MntTotal>11900</MntTotal>");
            // detalle
            assertThat(xml).contains("<NroLinDet>1</NroLinDet>");
            assertThat(xml).contains("<NmbItem>Producto uno</NmbItem>");
            assertThat(xml).contains("<QtyItem>2</QtyItem>");
            assertThat(xml).contains("<PrcItem>5000</PrcItem>");
            assertThat(xml).contains("<MontoItem>10000</MontoItem>");
            // observaciones
            assertThat(xml).contains("<Observaciones>Observación de prueba</Observaciones>");

            verify(documentoService).obtenerDetalle(DOC_ID);
            verify(empresaRepository).findById(EMPRESA_ID);
            verify(tenantService).getEmpresaId();
        }

        @Test
        @DisplayName("usa los datos vivos de la empresa cuando el snapshot del emisor está vacío")
        void generarXml_usaDatosVivosEmisorSinSnapshot() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setRutEmisor(null);
            doc.setRazonSocialEmisor("   "); // blank -> fallback
            doc.setGiroEmisor(null);
            doc.setDireccionEmisor(null);
            doc.setComunaEmisor(null);
            doc.setCiudadEmisor(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).contains("<RUTEmisor>76123456-7</RUTEmisor>");
            assertThat(xml).contains("<RznSoc>Empresa Emisora SpA</RznSoc>");
            assertThat(xml).contains("<GiroEmis>Servicios informáticos</GiroEmis>");
            assertThat(xml).contains("<DirOrigen>Av. Siempre Viva 742</DirOrigen>");
            assertThat(xml).contains("<CmnaOrigen>Providencia</CmnaOrigen>");
            assertThat(xml).contains("<CiudadOrigen>Santiago</CiudadOrigen>");
        }

        @Test
        @DisplayName("usa los datos vivos del cliente cuando el snapshot del receptor está vacío")
        void generarXml_usaDatosVivosReceptorSinSnapshot() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setRut(null);
            doc.setRazonSocial(null);
            doc.setGiro(null);
            doc.setDireccion(null);
            doc.setComuna(null);
            doc.setCiudad(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            // cae al cliente vivo del documento
            assertThat(xml).contains("<RUTRecep>11111111-1</RUTRecep>");
            assertThat(xml).contains("<RznSocRecep>Cliente Receptor Ltda</RznSocRecep>");
            assertThat(xml).contains("<GiroRecep>Comercio</GiroRecep>");
            assertThat(xml).contains("<DirRecep>Calle Falsa 123</DirRecep>");
        }

        @Test
        @DisplayName("receptor (cliente) nulo: campos del receptor quedan vacíos sin fallar")
        void generarXml_receptorNulo() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setCliente(null);
            // sin snapshot del receptor tampoco
            doc.setRut(null);
            doc.setRazonSocial(null);
            doc.setGiro(null);
            doc.setDireccion(null);
            doc.setComuna(null);
            doc.setCiudad(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            // los elementos existen pero vacíos
            assertThat(xml).contains("<RUTRecep/>");
            assertThat(xml).contains("<RznSocRecep/>");
        }

        @Test
        @DisplayName("folio nulo se serializa como 0")
        void generarXml_folioNulo() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setFolio(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).contains("<Folio>0</Folio>");
        }

        @Test
        @DisplayName("estado nulo y moneda nula caen a vacío y CLP por defecto")
        void generarXml_estadoYMonedaNulos() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setEstado(null);
            doc.setMoneda(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).contains("<Estado/>");
            assertThat(xml).contains("<MntMoneda>CLP</MntMoneda>");
        }

        @Test
        @DisplayName("fechas, tipo de cambio y montos nulos producen elementos vacíos")
        void generarXml_fechasYMontosNulos() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setFechaEmision(null);
            doc.setFechaVencimiento(null);
            doc.setTipoCambio(null);
            doc.setMontoNeto(null);
            doc.setMontoIva(null);
            doc.setMontoTotal(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).contains("<FchEmis/>");
            assertThat(xml).contains("<FchVenc/>");
            assertThat(xml).contains("<TpoCambio/>");
            assertThat(xml).contains("<MntNeto/>");
            assertThat(xml).contains("<IVA/>");
            assertThat(xml).contains("<MntTotal/>");
        }

        @Test
        @DisplayName("detalles nulos: no se emite ningún nodo Detalle y el XML sigue siendo válido")
        void generarXml_detallesNulos() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setDetalles(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).doesNotContain("<Detalle>");
            assertThat(xml).contains("<MntTotal>11900</MntTotal>");
        }

        @Test
        @DisplayName("varios detalles incrementan NroLinDet y descripción nula queda vacía")
        void generarXml_variosDetalles() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setDetalles(new ArrayList<>(List.of(
                    detalle("P001", "Item A", "1", "100", "100"),
                    detalle(null, null, "2", "200", "400"))));
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).contains("<NroLinDet>1</NroLinDet>");
            assertThat(xml).contains("<NmbItem>Item A</NmbItem>");
            assertThat(xml).contains("<NroLinDet>2</NroLinDet>");
            // descripción nula del segundo detalle -> NmbItem vacío
            assertThat(xml).contains("<NmbItem/>");
        }

        @Test
        @DisplayName("observaciones nulas: no se incluye el nodo Observaciones")
        void generarXml_sinObservaciones() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setObservaciones(null);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            String xml = xml(service.generarXml(DOC_ID));

            assertThat(xml).doesNotContain("<Observaciones>");
        }

        @Test
        @DisplayName("propaga RecursoNoEncontradoException si el documento no existe")
        void generarXml_documentoNoEncontrado() {
            when(documentoService.obtenerDetalle(DOC_ID))
                    .thenThrow(new RecursoNoEncontradoException("Documento no encontrado con id: " + DOC_ID));

            assertThatThrownBy(() -> service.generarXml(DOC_ID))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Documento no encontrado");

            verifyNoInteractions(empresaRepository);
            verify(tenantService, never()).getEmpresaId();
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si la empresa emisora no existe")
        void generarXml_empresaEmisoraNoEncontrada() {
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(documentoCompleto());
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generarXml(DOC_ID))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Empresa emisora no encontrada");

            verify(empresaRepository).findById(EMPRESA_ID);
        }

        @Test
        @DisplayName("propaga la excepción de tenant si no hay empresa en el contexto")
        void generarXml_sinTenant() {
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(documentoCompleto());
            when(tenantService.getEmpresaId())
                    .thenThrow(new RuntimeException("No se encontró empresa en el contexto del tenant"));

            assertThatThrownBy(() -> service.generarXml(DOC_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("contexto del tenant");

            verify(empresaRepository, never()).findById(anyLong());
        }
    }

    // ============================================================ generarPdf

    @Nested
    @DisplayName("generarPdf")
    class GenerarPdf {

        @Test
        @DisplayName("camino feliz: genera bytes de un PDF (cabecera %PDF)")
        void generarPdf_caminoFeliz() {
            DocumentoTributarioEntity doc = documentoCompleto();
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            byte[] bytes = service.generarPdf(DOC_ID);

            assertThat(bytes).isNotEmpty();
            // los PDF comienzan con la firma "%PDF"
            String cabecera = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            assertThat(cabecera).isEqualTo("%PDF");

            verify(documentoService).obtenerDetalle(DOC_ID);
            verify(empresaRepository).findById(EMPRESA_ID);
            verify(tenantService).getEmpresaId();
        }

        @Test
        @DisplayName("genera PDF aun en BORRADOR sin folio, sin detalles ni cliente")
        void generarPdf_borradorMinimo() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setEstado(EstadoDocumento.BORRADOR);
            doc.setFolio(null);
            doc.setCliente(null);
            doc.setDetalles(new ArrayList<>());
            doc.setReferencias(null);
            doc.setObservaciones(null);
            doc.setMoneda(null);
            doc.setMontoIva(BigDecimal.ZERO);
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            byte[] bytes = service.generarPdf(DOC_ID);

            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("genera PDF con referencias a otros documentos")
        void generarPdf_conReferencias() {
            DocumentoTributarioEntity doc = documentoCompleto();

            DocumentoTributarioEntity referido = new DocumentoTributarioEntity();
            referido.setId(7L);
            referido.setTipoDocumento(tipoDocumento(33, "Factura Electrónica"));
            referido.setFolio(7);
            referido.setFechaEmision(LocalDate.of(2025, 12, 1));

            ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
            ref.setDocumentoReferenciado(referido);
            ref.setTipoReferencia("1");
            ref.setMotivo("Anula factura anterior");
            doc.setReferencias(new ArrayList<>(List.of(ref)));

            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            byte[] bytes = service.generarPdf(DOC_ID);

            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("genera PDF con descripción de tipo nula (usa fallback 'Documento <codigo>')")
        void generarPdf_tipoSinDescripcion() {
            DocumentoTributarioEntity doc = documentoCompleto();
            doc.setTipoDocumento(tipoDocumento(56, null));
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(emisor));

            byte[] bytes = service.generarPdf(DOC_ID);

            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("propaga RecursoNoEncontradoException si el documento no existe")
        void generarPdf_documentoNoEncontrado() {
            when(documentoService.obtenerDetalle(DOC_ID))
                    .thenThrow(new RecursoNoEncontradoException("Documento no encontrado con id: " + DOC_ID));

            assertThatThrownBy(() -> service.generarPdf(DOC_ID))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Documento no encontrado");

            verifyNoInteractions(empresaRepository);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si la empresa emisora no existe")
        void generarPdf_empresaEmisoraNoEncontrada() {
            when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(documentoCompleto());
            when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generarPdf(DOC_ID))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Empresa emisora no encontrada");
        }
    }

    // ============================================================ emisor() multi-tenant

    @Test
    @DisplayName("emisor(): usa el empresaId del tenant para buscar la empresa")
    void emisor_usaEmpresaIdDelTenant() {
        DocumentoTributarioEntity doc = documentoCompleto();
        long otroTenant = 99L;
        EmpresaEntity otra = empresaCompleta();
        otra.setId(otroTenant);

        when(documentoService.obtenerDetalle(DOC_ID)).thenReturn(doc);
        when(tenantService.getEmpresaId()).thenReturn(otroTenant);
        when(empresaRepository.findById(otroTenant)).thenReturn(Optional.of(otra));

        service.generarXml(DOC_ID);

        verify(tenantService).getEmpresaId();
        verify(empresaRepository).findById(otroTenant);
        verify(empresaRepository, never()).findById(EMPRESA_ID);
    }

    @Test
    @DisplayName("no interactúa con el tenant ni la empresa si el documento no se puede obtener")
    void noTocaTenantSiDocumentoFalla() {
        // 'lenient' por si el stub no llega a usarse en alguna ruta
        lenient().when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);
        when(documentoService.obtenerDetalle(DOC_ID))
                .thenThrow(new RecursoNoEncontradoException("Documento no encontrado"));

        assertThatThrownBy(() -> service.generarPdf(DOC_ID))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(empresaRepository);
    }
}
