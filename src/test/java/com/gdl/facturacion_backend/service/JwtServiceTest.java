package com.gdl.facturacion_backend.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test unitario para {@link JwtService}.
 *
 * El servicio no tiene colaboradores (repositorios/servicios) que mockear: solo
 * depende de dos campos {@code @Value} ({@code SECRET_KEY} y {@code expirationMs})
 * que se inyectan mediante {@link ReflectionTestUtils}. Se cubren:
 *  - generateToken con y sin empresaId (las dos ramas del if).
 *  - extractUsername / extractEmpresaId / extractRol en caminos felices.
 *  - extractEmpresaId y extractRol cuando el claim es null (rama del ternario null).
 *  - Excepciones JWT: firma inválida (otra clave), token manipulado, token malformado
 *    y token expirado.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET =
            "TestSecretKeyForJUnit_0123456789_0123456789_0123456789_ABCDEFGH";
    private static final long EXPIRATION_MS = 3_600_000L;

    private static final String USERNAME = "usuario@test.cl";
    private static final Long EMPRESA_ID = 1L;
    private static final String ROL = "ADMIN";

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // generateToken
    // ------------------------------------------------------------------

    @Test
    void generateToken_conEmpresaId_incluyeTodosLosClaims() {
        String token = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);

        assertThat(token).isNotBlank();
        // Tres segmentos separados por punto: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);

        var claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(USERNAME);
        assertThat(claims.get("rol")).isEqualTo(ROL);
        assertThat(Long.valueOf(claims.get("empresaId").toString())).isEqualTo(EMPRESA_ID);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void generateToken_sinEmpresaId_noIncluyeClaimEmpresaId() {
        String token = jwtService.generateToken(USERNAME, null, ROL);

        assertThat(token).isNotBlank();

        var claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(USERNAME);
        assertThat(claims.get("rol")).isEqualTo(ROL);
        assertThat(claims.get("empresaId")).isNull();
    }

    @Test
    void generateToken_fijaExpiracionSegunExpirationMs() {
        long antes = System.currentTimeMillis();
        String token = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);
        long despues = System.currentTimeMillis();

        var claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long expiracion = claims.getExpiration().getTime();
        // El claim "exp" del JWT se serializa con precision de segundos, por lo que
        // los milisegundos se truncan. La expiracion debe caer (con holgura de 1s)
        // dentro de [antes+exp, despues+exp].
        assertThat(expiracion)
                .isGreaterThanOrEqualTo(antes + EXPIRATION_MS - 1000)
                .isLessThanOrEqualTo(despues + EXPIRATION_MS);
    }

    @Test
    void generateToken_dosTokensDistintosSonValidos() {
        String t1 = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);
        String t2 = jwtService.generateToken("otro@test.cl", 2L, "USER");

        assertThat(jwtService.extractUsername(t1)).isEqualTo(USERNAME);
        assertThat(jwtService.extractUsername(t2)).isEqualTo("otro@test.cl");
        assertThat(jwtService.extractEmpresaId(t2)).isEqualTo(2L);
        assertThat(jwtService.extractRol(t2)).isEqualTo("USER");
    }

    // ------------------------------------------------------------------
    // extractUsername
    // ------------------------------------------------------------------

    @Test
    void extractUsername_devuelveSubjectDelToken() {
        String token = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);

        assertThat(jwtService.extractUsername(token)).isEqualTo(USERNAME);
    }

    // ------------------------------------------------------------------
    // extractEmpresaId
    // ------------------------------------------------------------------

    @Test
    void extractEmpresaId_devuelveEmpresaIdCuandoExiste() {
        String token = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);

        assertThat(jwtService.extractEmpresaId(token)).isEqualTo(EMPRESA_ID);
    }

    @Test
    void extractEmpresaId_devuelveNullCuandoNoHayClaim() {
        String token = jwtService.generateToken(USERNAME, null, ROL);

        assertThat(jwtService.extractEmpresaId(token)).isNull();
    }

    // ------------------------------------------------------------------
    // extractRol
    // ------------------------------------------------------------------

    @Test
    void extractRol_devuelveRolCuandoExiste() {
        String token = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);

        assertThat(jwtService.extractRol(token)).isEqualTo(ROL);
    }

    @Test
    void extractRol_devuelveNullCuandoClaimRolEsNull() {
        // Token firmado manualmente con la misma clave pero sin el claim "rol".
        String token = Jwts.builder()
                .subject(USERNAME)
                .claims(new HashMap<>())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(signingKey())
                .compact();

        assertThat(jwtService.extractRol(token)).isNull();
    }

    // ------------------------------------------------------------------
    // Excepciones JWT
    // ------------------------------------------------------------------

    @Test
    void extractUsername_lanzaExcepcionCuandoFirmaConOtraClave() {
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "ClaveDistintaParaFirmarElTokenInvalido_0123456789_ABCDEFGH"
                        .getBytes(StandardCharsets.UTF_8));

        String tokenAjeno = Jwts.builder()
                .subject(USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(otraClave)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(tokenAjeno))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractRol_lanzaExcepcionCuandoTokenEsManipulado() {
        String token = jwtService.generateToken(USERNAME, EMPRESA_ID, ROL);
        // Alteramos el payload para romper la firma.
        String[] partes = token.split("\\.");
        String manipulado = partes[0] + "." + partes[1] + "X." + partes[2];

        assertThatThrownBy(() -> jwtService.extractRol(manipulado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractEmpresaId_lanzaExcepcionCuandoTokenEsMalformado() {
        assertThatThrownBy(() -> jwtService.extractEmpresaId("esto-no-es-un-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUsername_lanzaExcepcionCuandoTokenEstaExpirado() {
        // Token ya expirado (expiration en el pasado), firmado con la clave correcta.
        long ahora = System.currentTimeMillis();
        String tokenExpirado = Jwts.builder()
                .subject(USERNAME)
                .issuedAt(new Date(ahora - 10_000))
                .expiration(new Date(ahora - 5_000))
                .signWith(signingKey())
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(tokenExpirado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractMetodos_lanzanExcepcionConTokenNuloOVacio() {
        assertThatThrownBy(() -> jwtService.extractUsername(""))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jwtService.extractEmpresaId(""))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jwtService.extractRol(""))
                .isInstanceOf(Exception.class);
    }

    // ------------------------------------------------------------------
    // Coherencia de claims sin perdida de informacion
    // ------------------------------------------------------------------

    @Test
    void roundTrip_generaYExtraeTodosLosClaims() {
        Map<String, Object> esperado = new HashMap<>();
        esperado.put("username", "round@trip.cl");
        esperado.put("empresaId", 99L);
        esperado.put("rol", "SUPERVISOR");

        String token = jwtService.generateToken(
                (String) esperado.get("username"),
                (Long) esperado.get("empresaId"),
                (String) esperado.get("rol"));

        assertThat(jwtService.extractUsername(token)).isEqualTo(esperado.get("username"));
        assertThat(jwtService.extractEmpresaId(token)).isEqualTo(esperado.get("empresaId"));
        assertThat(jwtService.extractRol(token)).isEqualTo(esperado.get("rol"));
    }
}
