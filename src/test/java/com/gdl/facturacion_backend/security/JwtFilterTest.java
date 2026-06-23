package com.gdl.facturacion_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test unitario para {@link JwtFilter}.
 *
 * <p>El filtro extiende {@code OncePerRequestFilter} y depende de un colaborador
 * {@link JwtService} inyectado por campo ({@code @Autowired}), por lo que se mockea
 * con {@code @Mock} + {@code @InjectMocks}. La peticion, la respuesta y la cadena de
 * filtros se mockean con Mockito.</p>
 *
 * <p>Los metodos {@code doFilterInternal} y {@code shouldNotFilter} son {@code protected},
 * pero el test vive en el mismo paquete que el filtro, por lo que pueden invocarse
 * directamente sin reflexion.</p>
 *
 * <p>El filtro usa estado estatico ({@link SecurityContextHolder} y {@link TenantContext}),
 * de modo que se limpia antes y despues de cada test para evitar fugas entre casos.</p>
 *
 * <p>Casos cubiertos:</p>
 * <ul>
 *   <li>Header "Bearer &lt;token&gt;" valido: setea Authentication con la authority del rol
 *       y el empresaId en el TenantContext; llama a doFilter.</li>
 *   <li>Header valido sin empresaId (null): no fija el tenant pero si autentica.</li>
 *   <li>Header valido sin rol (null): autentica con lista de authorities vacia.</li>
 *   <li>Sin header Authorization: no autentica, no toca JwtService, pero llama a doFilter.</li>
 *   <li>Header presente pero sin prefijo "Bearer ": no autentica.</li>
 *   <li>TenantContext se limpia siempre al final (camino feliz y cuando JwtService lanza).</li>
 *   <li>doFilter se invoca siempre exactamente una vez en el camino feliz.</li>
 *   <li>shouldNotFilter: true para /auth y /api/tipos-documento; false para otras rutas.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private static final String USERNAME = "usuario@test.cl";
    private static final Long EMPRESA_ID = 7L;
    private static final String ROL = "ADMIN";
    private static final String TOKEN = "token.jwt.valido";
    private static final String BEARER = "Bearer " + TOKEN;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        // Estado estatico limpio antes de cada caso.
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // doFilterInternal - camino feliz
    // ------------------------------------------------------------------

    @Test
    void doFilterInternal_conTokenValido_seteaAuthenticationConRolYTenant() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER);
        when(jwtService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtService.extractEmpresaId(TOKEN)).thenReturn(EMPRESA_ID);
        when(jwtService.extractRol(TOKEN)).thenReturn(ROL);

        // Capturamos el authentication y el tenant DENTRO de la cadena, porque
        // el bloque finally limpia el TenantContext al terminar el filtro.
        final Authentication[] authDurante = new Authentication[1];
        final Long[] tenantDurante = new Long[1];
        doCapture(authDurante, tenantDurante);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Authentication seteado con el username como principal y la authority del rol.
        Authentication auth = authDurante[0];
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(USERNAME);
        assertThat(auth.getCredentials()).isNull();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROL);

        // El TenantContext estuvo seteado mientras se procesaba la cadena.
        assertThat(tenantDurante[0]).isEqualTo(EMPRESA_ID);

        // doFilter se invoca exactamente una vez con los mismos request/response.
        verify(filterChain, times(1)).doFilter(request, response);

        // Tras finalizar, el TenantContext queda limpio (bloque finally).
        assertThat(TenantContext.getEmpresaId()).isNull();
    }

    @Test
    void doFilterInternal_conTokenValidoSinEmpresaId_noFijaTenantPeroAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER);
        when(jwtService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtService.extractEmpresaId(TOKEN)).thenReturn(null);
        when(jwtService.extractRol(TOKEN)).thenReturn(ROL);

        final Authentication[] authDurante = new Authentication[1];
        final Long[] tenantDurante = new Long[1];
        doCapture(authDurante, tenantDurante);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(authDurante[0]).isNotNull();
        assertThat(authDurante[0].getPrincipal()).isEqualTo(USERNAME);
        // Rama del if (empresaId == null): nunca se fija el tenant.
        assertThat(tenantDurante[0]).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(TenantContext.getEmpresaId()).isNull();
    }

    @Test
    void doFilterInternal_conTokenValidoSinRol_autenticaSinAuthorities() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER);
        when(jwtService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtService.extractEmpresaId(TOKEN)).thenReturn(EMPRESA_ID);
        when(jwtService.extractRol(TOKEN)).thenReturn(null);

        final Authentication[] authDurante = new Authentication[1];
        final Long[] tenantDurante = new Long[1];
        doCapture(authDurante, tenantDurante);

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = authDurante[0];
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(USERNAME);
        // Rama del ternario (rol == null): lista de authorities vacia.
        assertThat(auth.getAuthorities()).isEmpty();
        assertThat(tenantDurante[0]).isEqualTo(EMPRESA_ID);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(TenantContext.getEmpresaId()).isNull();
    }

    // ------------------------------------------------------------------
    // doFilterInternal - sin autenticacion
    // ------------------------------------------------------------------

    @Test
    void doFilterInternal_sinHeader_noAutenticaPeroLlamaDoFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // No se autentica.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // No se consulta a JwtService cuando no hay header.
        verifyNoInteractions(jwtService);
        // Pero la cadena de filtros continua igualmente.
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(TenantContext.getEmpresaId()).isNull();
    }

    @Test
    void doFilterInternal_headerSinPrefijoBearer_noAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(TenantContext.getEmpresaId()).isNull();
    }

    @Test
    void doFilterInternal_headerVacio_noAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ------------------------------------------------------------------
    // doFilterInternal - limpieza del TenantContext en el bloque finally
    // ------------------------------------------------------------------

    @Test
    void doFilterInternal_cuandoJwtServiceLanza_limpiaTenantYNoLlamaDoFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER);
        when(jwtService.extractUsername(TOKEN))
                .thenThrow(new JwtException("token invalido"));

        // La excepcion se propaga (el filtro no la captura), pero el finally
        // limpia el TenantContext igualmente.
        assertThatThrownBy(() -> jwtFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(JwtException.class);

        // El tenant nunca llego a setearse (la excepcion ocurre antes), y el
        // finally garantiza que quede limpio.
        assertThat(TenantContext.getEmpresaId()).isNull();
        // Como la excepcion ocurre antes del doFilter, la cadena no continua.
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_caminoFeliz_limpiaTenantContextAlFinal() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER);
        when(jwtService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtService.extractEmpresaId(TOKEN)).thenReturn(EMPRESA_ID);
        when(jwtService.extractRol(TOKEN)).thenReturn(ROL);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Tras un procesamiento exitoso el tenant siempre se limpia.
        assertThat(TenantContext.getEmpresaId()).isNull();
    }

    // ------------------------------------------------------------------
    // shouldNotFilter
    // ------------------------------------------------------------------

    @Test
    void shouldNotFilter_paraRutaAuth_devuelveTrue() {
        when(request.getServletPath()).thenReturn("/auth/login");

        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_paraRutaTiposDocumento_devuelveTrue() {
        when(request.getServletPath()).thenReturn("/api/tipos-documento/1");

        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_paraRutaProtegida_devuelveFalse() {
        when(request.getServletPath()).thenReturn("/api/documentos");

        assertThat(jwtFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_paraRutaRaiz_devuelveFalse() {
        when(request.getServletPath()).thenReturn("/");

        assertThat(jwtFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_rutaQueEmpiezaConAuth_devuelveTrue() {
        // startsWith("/auth") cubre rutas como "/authentication" tambien.
        when(request.getServletPath()).thenReturn("/authentication/registro");

        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Configura {@code filterChain.doFilter} para capturar el {@link Authentication}
     * vigente y el empresaId del {@link TenantContext} en el momento en que la cadena
     * se ejecuta. Es necesario porque el bloque {@code finally} del filtro limpia el
     * TenantContext apenas termina, de modo que tras la llamada ya no podriamos leerlo.
     */
    private void doCapture(Authentication[] authHolder, Long[] tenantHolder) throws Exception {
        // lenient: algunos tests usan este helper sin verificar la captura del tenant.
        lenient().doAnswer(invocation -> {
            authHolder[0] = SecurityContextHolder.getContext().getAuthentication();
            tenantHolder[0] = TenantContext.getEmpresaId();
            return null;
        }).when(filterChain).doFilter(eq(request), eq(response));
    }
}
