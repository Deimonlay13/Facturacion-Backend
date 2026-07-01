package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioCreateRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioUpdateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.exception.CredencialesInvalidasException;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.UsuarioRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final String ROL_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROL_ADMIN = "ROLE_ADMIN";
    private static final String ROL_USER = "ROLE_USER";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private RolService roleService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        TenantContext.setEmpresaId(EMPRESA_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private RolEntity rol(String nombre) {
        RolEntity rol = new RolEntity();
        rol.setNombre(nombre);
        return rol;
    }

    private EmpresaEntity empresa(Long id) {
        EmpresaEntity e = new EmpresaEntity();
        e.setId(id);
        return e;
    }

    private UsuarioEntity usuario(Long id, String username) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private RegisterRequest registerRequest(String username, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    private UsuarioCreateRequest createRequest(String username, String password, String rol) {
        UsuarioCreateRequest r = new UsuarioCreateRequest();
        r.setUsername(username);
        r.setPassword(password);
        r.setRol(rol);
        return r;
    }

    private UsuarioUpdateRequest updateRequest(String username, String password) {
        UsuarioUpdateRequest r = new UsuarioUpdateRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    // ------------------------------------------------------------------
    // register
    // ------------------------------------------------------------------

    @Test
    void register_primerUsuario_asignaRolAdminYRetornaTokens() {
        RegisterRequest request = registerRequest("nuevo", "secreto");
        EmpresaEntity empresa = empresa(EMPRESA_ID);
        RolEntity rolAdmin = rol(ROL_ADMIN);

        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);
        when(empresaService.findById(EMPRESA_ID)).thenReturn(empresa);
        when(usuarioRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of());
        when(roleService.findByNombre(ROL_ADMIN)).thenReturn(rolAdmin);
        when(passwordEncoder.encode("secreto")).thenReturn("hash-secreto");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("nuevo", EMPRESA_ID, ROL_ADMIN)).thenReturn("access-token");
        when(refreshTokenService.crear(any(UsuarioEntity.class))).thenReturn("refresh-token");

        AuthResponse response = usuarioService.register(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity guardado = captor.getValue();
        assertThat(guardado.getUsername()).isEqualTo("nuevo");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-secreto");
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getEmpresa()).isSameAs(empresa);
        assertThat(guardado.getRol()).isSameAs(rolAdmin);

        verify(roleService).findByNombre(ROL_ADMIN);
        verify(jwtService).generateToken("nuevo", EMPRESA_ID, ROL_ADMIN);
        verify(refreshTokenService).crear(guardado);
    }

    @Test
    void register_segundoUsuario_asignaRolUser() {
        RegisterRequest request = registerRequest("otro", "clave");
        RolEntity rolUser = rol(ROL_USER);

        when(usuarioRepository.existsByUsername("otro")).thenReturn(false);
        when(empresaService.findById(EMPRESA_ID)).thenReturn(empresa(EMPRESA_ID));
        when(usuarioRepository.findAllByEmpresaId(EMPRESA_ID))
                .thenReturn(List.of(usuario(99L, "existente")));
        when(roleService.findByNombre(ROL_USER)).thenReturn(rolUser);
        when(passwordEncoder.encode("clave")).thenReturn("hash-clave");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("otro", EMPRESA_ID, ROL_USER)).thenReturn("token");
        when(refreshTokenService.crear(any(UsuarioEntity.class))).thenReturn("refresh");

        AuthResponse response = usuarioService.register(request);

        assertThat(response.getToken()).isEqualTo("token");
        verify(roleService).findByNombre(ROL_USER);
        verify(roleService, never()).findByNombre(ROL_ADMIN);
    }

    @Test
    void register_lanzaReglaNegocio_cuandoUsernameYaExiste() {
        RegisterRequest request = registerRequest("repetido", "x");
        when(usuarioRepository.existsByUsername("repetido")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(empresaService, roleService, jwtService, refreshTokenService);
    }

    @Test
    void register_lanzaReglaNegocio_cuandoFaltaTenant() {
        TenantContext.clear();
        RegisterRequest request = registerRequest("nuevo", "x");
        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Operación no permitida: falta identificación de empresa (X-Tenant-ID o JWT)");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(empresaService);
    }

    @Test
    void register_propagaRecursoNoEncontrado_cuandoEmpresaNoExiste() {
        RegisterRequest request = registerRequest("nuevo", "x");
        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);
        when(empresaService.findById(EMPRESA_ID))
                .thenThrow(new RecursoNoEncontradoException("Empresa no encontrada con id: 1"));

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Empresa no encontrada con id: 1");

        verify(usuarioRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // login
    // ------------------------------------------------------------------

    @Test
    void login_retornaTokens_cuandoCredencialesValidas() {
        LoginRequest request = loginRequest("juan", "pass");
        EmpresaEntity empresa = empresa(EMPRESA_ID);
        RolEntity rol = rol(ROL_USER);
        UsuarioEntity usuario = usuario(10L, "juan");
        usuario.setPasswordHash("hash-en-bd");
        usuario.setEmpresa(empresa);
        usuario.setRol(rol);

        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass", "hash-en-bd")).thenReturn(true);
        when(jwtService.generateToken("juan", EMPRESA_ID, ROL_USER)).thenReturn("access");
        when(refreshTokenService.crear(usuario)).thenReturn("refresh");

        AuthResponse response = usuarioService.login(request);

        assertThat(response.getToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(jwtService).generateToken("juan", EMPRESA_ID, ROL_USER);
        verify(refreshTokenService).crear(usuario);
    }

    @Test
    void login_usaEmpresaIdNull_cuandoUsuarioSinEmpresa() {
        LoginRequest request = loginRequest("root", "pass");
        UsuarioEntity usuario = usuario(1L, "root");
        usuario.setPasswordHash("hash");
        usuario.setEmpresa(null);
        usuario.setRol(rol(ROL_SUPER_ADMIN));

        when(usuarioRepository.findByUsername("root")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(jwtService.generateToken("root", null, ROL_SUPER_ADMIN)).thenReturn("access");
        when(refreshTokenService.crear(usuario)).thenReturn("refresh");

        AuthResponse response = usuarioService.login(request);

        assertThat(response.getToken()).isEqualTo("access");
        verify(jwtService).generateToken("root", null, ROL_SUPER_ADMIN);
    }

    @Test
    void login_lanzaCredencialesInvalidas_cuandoUsuarioNoExiste() {
        LoginRequest request = loginRequest("fantasma", "pass");
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.login(request))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Usuario o contraseña inválidos");

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void login_lanzaCredencialesInvalidas_cuandoPasswordNoCoincide() {
        LoginRequest request = loginRequest("juan", "mala");
        UsuarioEntity usuario = usuario(10L, "juan");
        usuario.setPasswordHash("hash-en-bd");

        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash-en-bd")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.login(request))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Usuario o contraseña inválidos");

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    // ------------------------------------------------------------------
    // listar
    // ------------------------------------------------------------------

    @Test
    void listar_retornaUsuariosDeLaEmpresaActual() {
        UsuarioEntity u1 = usuario(1L, "a");
        UsuarioEntity u2 = usuario(2L, "b");
        when(usuarioRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of(u1, u2));

        List<UsuarioEntity> resultado = usuarioService.listar();

        assertThat(resultado).containsExactly(u1, u2);
        verify(usuarioRepository).findAllByEmpresaId(EMPRESA_ID);
    }

    @Test
    void listar_retornaListaVacia_cuandoNoHayUsuarios() {
        when(usuarioRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        assertThat(usuarioService.listar()).isEmpty();
    }

    @Test
    void listar_lanzaReglaNegocio_cuandoFaltaTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> usuarioService.listar())
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Operación no permitida: falta identificación de empresa (X-Tenant-ID o JWT)");

        verifyNoInteractions(usuarioRepository);
    }

    // ------------------------------------------------------------------
    // obtenerPorId
    // ------------------------------------------------------------------

    @Test
    void obtenerPorId_retornaUsuario_cuandoExiste() {
        UsuarioEntity usuario = usuario(5L, "juan");
        when(usuarioRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(usuario));

        UsuarioEntity resultado = usuarioService.obtenerPorId(5L);

        assertThat(resultado).isSameAs(usuario);
        verify(usuarioRepository).findByIdAndEmpresaId(5L, EMPRESA_ID);
    }

    @Test
    void obtenerPorId_lanzaRecursoNoEncontrado_cuandoNoExiste() {
        when(usuarioRepository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Usuario no encontrado con id: 99");
    }

    // ------------------------------------------------------------------
    // crear
    // ------------------------------------------------------------------

    @Test
    void crear_usaRolUserPorDefecto_cuandoRolNull() {
        UsuarioCreateRequest request = createRequest("nuevo", "pass", null);
        RolEntity rolUser = rol(ROL_USER);
        EmpresaEntity empresa = empresa(EMPRESA_ID);

        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);
        when(roleService.findByNombre(ROL_USER)).thenReturn(rolUser);
        when(empresaService.findById(EMPRESA_ID)).thenReturn(empresa);
        when(passwordEncoder.encode("pass")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.crear(request);

        assertThat(resultado.getUsername()).isEqualTo("nuevo");
        assertThat(resultado.getPasswordHash()).isEqualTo("hash");
        assertThat(resultado.getActivo()).isTrue();
        assertThat(resultado.getRol()).isSameAs(rolUser);
        assertThat(resultado.getEmpresa()).isSameAs(empresa);
        verify(roleService).findByNombre(ROL_USER);
        verify(empresaService).findById(EMPRESA_ID);
    }

    @Test
    void crear_usaRolUserPorDefecto_cuandoRolEnBlanco() {
        UsuarioCreateRequest request = createRequest("nuevo", "pass", "   ");
        RolEntity rolUser = rol(ROL_USER);

        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);
        when(roleService.findByNombre(ROL_USER)).thenReturn(rolUser);
        when(empresaService.findById(EMPRESA_ID)).thenReturn(empresa(EMPRESA_ID));
        when(passwordEncoder.encode("pass")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.crear(request);

        verify(roleService).findByNombre(ROL_USER);
    }

    @Test
    void crear_usaRolIndicado_cuandoSeProporciona() {
        UsuarioCreateRequest request = createRequest("admin2", "pass", ROL_ADMIN);
        RolEntity rolAdmin = rol(ROL_ADMIN);
        EmpresaEntity empresa = empresa(EMPRESA_ID);

        when(usuarioRepository.existsByUsername("admin2")).thenReturn(false);
        when(roleService.findByNombre(ROL_ADMIN)).thenReturn(rolAdmin);
        when(empresaService.findById(EMPRESA_ID)).thenReturn(empresa);
        when(passwordEncoder.encode("pass")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.crear(request);

        assertThat(resultado.getRol()).isSameAs(rolAdmin);
        assertThat(resultado.getEmpresa()).isSameAs(empresa);
        verify(roleService).findByNombre(ROL_ADMIN);
    }

    @Test
    void crear_noAsignaEmpresa_cuandoRolSuperAdmin() {
        UsuarioCreateRequest request = createRequest("super", "pass", ROL_SUPER_ADMIN);
        RolEntity rolSuper = rol(ROL_SUPER_ADMIN);

        when(usuarioRepository.existsByUsername("super")).thenReturn(false);
        when(roleService.findByNombre(ROL_SUPER_ADMIN)).thenReturn(rolSuper);
        when(passwordEncoder.encode("pass")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.crear(request);

        assertThat(resultado.getRol()).isSameAs(rolSuper);
        assertThat(resultado.getEmpresa()).isNull();
        verify(empresaService, never()).findById(any());
    }

    @Test
    void crear_lanzaReglaNegocio_cuandoUsernameYaExiste() {
        UsuarioCreateRequest request = createRequest("repetido", "pass", ROL_USER);
        when(usuarioRepository.existsByUsername("repetido")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(roleService, empresaService, passwordEncoder);
    }

    // ------------------------------------------------------------------
    // actualizar
    // ------------------------------------------------------------------

    @Test
    void actualizar_cambiaUsernameYPassword() {
        UsuarioUpdateRequest request = updateRequest("nuevoNombre", "nuevaPass");
        UsuarioEntity usuario = usuario(3L, "viejoNombre");
        usuario.setPasswordHash("hash-viejo");

        when(usuarioRepository.findByIdAndEmpresaId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("nuevoNombre")).thenReturn(false);
        when(passwordEncoder.encode("nuevaPass")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.actualizar(3L, request);

        assertThat(resultado.getUsername()).isEqualTo("nuevoNombre");
        assertThat(resultado.getPasswordHash()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).existsByUsername("nuevoNombre");
        verify(passwordEncoder).encode("nuevaPass");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizar_noCambiaUsername_cuandoEsIgualAlActual() {
        UsuarioUpdateRequest request = updateRequest("mismoNombre", null);
        UsuarioEntity usuario = usuario(3L, "mismoNombre");
        usuario.setPasswordHash("hash");

        when(usuarioRepository.findByIdAndEmpresaId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.actualizar(3L, request);

        assertThat(resultado.getUsername()).isEqualTo("mismoNombre");
        // No debe verificar unicidad porque el username no cambió
        verify(usuarioRepository, never()).existsByUsername(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void actualizar_ignoraUsernameNull_yMantienePassword() {
        UsuarioUpdateRequest request = updateRequest(null, null);
        UsuarioEntity usuario = usuario(3L, "actual");
        usuario.setPasswordHash("hash-actual");

        when(usuarioRepository.findByIdAndEmpresaId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.actualizar(3L, request);

        assertThat(resultado.getUsername()).isEqualTo("actual");
        assertThat(resultado.getPasswordHash()).isEqualTo("hash-actual");
        verify(usuarioRepository, never()).existsByUsername(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void actualizar_ignoraUsernameEnBlanco() {
        UsuarioUpdateRequest request = updateRequest("   ", null);
        UsuarioEntity usuario = usuario(3L, "actual");

        when(usuarioRepository.findByIdAndEmpresaId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.actualizar(3L, request);

        assertThat(resultado.getUsername()).isEqualTo("actual");
        verify(usuarioRepository, never()).existsByUsername(anyString());
    }

    @Test
    void actualizar_ignoraPasswordEnBlanco() {
        UsuarioUpdateRequest request = updateRequest(null, "   ");
        UsuarioEntity usuario = usuario(3L, "actual");
        usuario.setPasswordHash("hash-actual");

        when(usuarioRepository.findByIdAndEmpresaId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.actualizar(3L, request);

        assertThat(resultado.getPasswordHash()).isEqualTo("hash-actual");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void actualizar_lanzaReglaNegocio_cuandoNuevoUsernameYaExiste() {
        UsuarioUpdateRequest request = updateRequest("ocupado", null);
        UsuarioEntity usuario = usuario(3L, "actual");

        when(usuarioRepository.findByIdAndEmpresaId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("ocupado")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(3L, request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_lanzaRecursoNoEncontrado_cuandoUsuarioNoExiste() {
        UsuarioUpdateRequest request = updateRequest("x", null);
        when(usuarioRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizar(7L, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Usuario no encontrado con id: 7");

        verify(usuarioRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // cambiarRol
    // ------------------------------------------------------------------

    @Test
    void cambiarRol_aSuperAdmin_quitaEmpresa() {
        UsuarioEntity usuario = usuario(4L, "juan");
        usuario.setEmpresa(empresa(EMPRESA_ID));
        RolEntity rolSuper = rol(ROL_SUPER_ADMIN);

        when(usuarioRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(roleService.findByNombre(ROL_SUPER_ADMIN)).thenReturn(rolSuper);
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.cambiarRol(4L, ROL_SUPER_ADMIN);

        assertThat(resultado.getRol()).isSameAs(rolSuper);
        assertThat(resultado.getEmpresa()).isNull();
        verify(empresaService, never()).findById(any());
    }

    @Test
    void cambiarRol_aRolNormal_asignaEmpresa_cuandoUsuarioSinEmpresa() {
        UsuarioEntity usuario = usuario(4L, "juan");
        usuario.setEmpresa(null);
        RolEntity rolUser = rol(ROL_USER);
        EmpresaEntity empresa = empresa(EMPRESA_ID);

        when(usuarioRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(roleService.findByNombre(ROL_USER)).thenReturn(rolUser);
        when(empresaService.findById(EMPRESA_ID)).thenReturn(empresa);
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.cambiarRol(4L, ROL_USER);

        assertThat(resultado.getRol()).isSameAs(rolUser);
        assertThat(resultado.getEmpresa()).isSameAs(empresa);
        verify(empresaService).findById(EMPRESA_ID);
    }

    @Test
    void cambiarRol_aRolNormal_mantieneEmpresa_cuandoUsuarioYaTieneEmpresa() {
        EmpresaEntity empresaExistente = empresa(EMPRESA_ID);
        UsuarioEntity usuario = usuario(4L, "juan");
        usuario.setEmpresa(empresaExistente);
        RolEntity rolUser = rol(ROL_USER);

        when(usuarioRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(roleService.findByNombre(ROL_USER)).thenReturn(rolUser);
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.cambiarRol(4L, ROL_USER);

        assertThat(resultado.getEmpresa()).isSameAs(empresaExistente);
        verify(empresaService, never()).findById(any());
    }

    @Test
    void cambiarRol_lanzaRecursoNoEncontrado_cuandoUsuarioNoExiste() {
        when(usuarioRepository.findByIdAndEmpresaId(50L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.cambiarRol(50L, ROL_USER))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Usuario no encontrado con id: 50");

        verifyNoInteractions(roleService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarRol_propagaExcepcion_cuandoRolNoExiste() {
        UsuarioEntity usuario = usuario(4L, "juan");
        when(usuarioRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(roleService.findByNombre("ROLE_X"))
                .thenThrow(new RuntimeException("El rol ROLE_X no existe en el sistema"));

        assertThatThrownBy(() -> usuarioService.cambiarRol(4L, "ROLE_X"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El rol ROLE_X no existe en el sistema");

        verify(usuarioRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // cambiarEstado
    // ------------------------------------------------------------------

    @Test
    void cambiarEstado_activaUsuario() {
        UsuarioEntity usuario = usuario(6L, "juan");
        usuario.setActivo(false);

        when(usuarioRepository.findByIdAndEmpresaId(6L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.cambiarEstado(6L, true);

        assertThat(resultado.getActivo()).isTrue();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarEstado_desactivaUsuario() {
        UsuarioEntity usuario = usuario(6L, "juan");
        usuario.setActivo(true);

        when(usuarioRepository.findByIdAndEmpresaId(6L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEntity resultado = usuarioService.cambiarEstado(6L, false);

        assertThat(resultado.getActivo()).isFalse();
    }

    @Test
    void cambiarEstado_lanzaRecursoNoEncontrado_cuandoUsuarioNoExiste() {
        when(usuarioRepository.findByIdAndEmpresaId(60L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.cambiarEstado(60L, true))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Usuario no encontrado con id: 60");

        verify(usuarioRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // eliminar
    // ------------------------------------------------------------------

    @Test
    void eliminar_borraUsuario_cuandoExisteYNoEsRoot() {
        UsuarioEntity usuario = usuario(8L, "juan");
        when(usuarioRepository.findByIdAndEmpresaId(8L, EMPRESA_ID)).thenReturn(Optional.of(usuario));

        usuarioService.eliminar(8L);

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void eliminar_lanzaReglaNegocio_cuandoUsuarioEsRoot() {
        UsuarioEntity usuario = usuario(1L, "root");
        when(usuarioRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar el super-usuario root.");

        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void eliminar_lanzaReglaNegocio_cuandoUsuarioEsRootSinImportarMayusculas() {
        UsuarioEntity usuario = usuario(1L, "ROOT");
        when(usuarioRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar el super-usuario root.");

        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void eliminar_lanzaRecursoNoEncontrado_cuandoUsuarioNoExiste() {
        when(usuarioRepository.findByIdAndEmpresaId(80L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.eliminar(80L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Usuario no encontrado con id: 80");

        verify(usuarioRepository, never()).delete(any());
    }
}
