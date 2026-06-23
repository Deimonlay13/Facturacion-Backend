package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.RefreshTokenEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ---------- Helpers ----------

    private RefreshTokenEntity persistToken(String token, Long usuarioId, LocalDateTime expiraEn, boolean revocado) {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken(token);
        rt.setUsuarioId(usuarioId);
        rt.setExpiraEn(expiraEn);
        rt.setRevocado(revocado);
        return entityManager.persistAndFlush(rt);
    }

    // ---------- findByToken ----------

    @Test
    void findByToken_existente_devuelveToken() {
        LocalDateTime expira = LocalDateTime.now().plusDays(7);
        RefreshTokenEntity guardado = persistToken("token-abc-123", 10L, expira, false);
        entityManager.clear();

        Optional<RefreshTokenEntity> resultado = refreshTokenRepository.findByToken("token-abc-123");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(guardado.getId());
        assertThat(resultado.get().getToken()).isEqualTo("token-abc-123");
        assertThat(resultado.get().getUsuarioId()).isEqualTo(10L);
        assertThat(resultado.get().isRevocado()).isFalse();
        assertThat(resultado.get().getExpiraEn()).isEqualTo(expira);
    }

    @Test
    void findByToken_inexistente_devuelveVacio() {
        persistToken("token-existente", 1L, LocalDateTime.now().plusDays(1), false);
        entityManager.clear();

        Optional<RefreshTokenEntity> resultado = refreshTokenRepository.findByToken("token-que-no-existe");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByToken_baseVacia_devuelveVacio() {
        Optional<RefreshTokenEntity> resultado = refreshTokenRepository.findByToken("cualquier-token");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByToken_esSensibleAMayusculas() {
        persistToken("token-MiNuScUlAs", 5L, LocalDateTime.now().plusDays(1), false);
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("token-MiNuScUlAs")).isPresent();
        assertThat(refreshTokenRepository.findByToken("TOKEN-MINUSCULAS")).isEmpty();
    }

    @Test
    void findByToken_recuperaTokenRevocado() {
        persistToken("token-revocado", 7L, LocalDateTime.now().plusDays(1), true);
        entityManager.clear();

        Optional<RefreshTokenEntity> resultado = refreshTokenRepository.findByToken("token-revocado");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().isRevocado()).isTrue();
    }

    @Test
    void findByToken_distingueEntreVariosTokens() {
        persistToken("token-uno", 1L, LocalDateTime.now().plusDays(1), false);
        persistToken("token-dos", 2L, LocalDateTime.now().plusDays(2), false);
        persistToken("token-tres", 3L, LocalDateTime.now().plusDays(3), true);
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("token-uno"))
                .get().extracting(RefreshTokenEntity::getUsuarioId).isEqualTo(1L);
        assertThat(refreshTokenRepository.findByToken("token-dos"))
                .get().extracting(RefreshTokenEntity::getUsuarioId).isEqualTo(2L);
        assertThat(refreshTokenRepository.findByToken("token-tres"))
                .get().extracting(RefreshTokenEntity::isRevocado).isEqualTo(true);
    }

    // ---------- persistencia / restricciones ----------

    @Test
    void guardarToken_persisteTodosLosCampos() {
        LocalDateTime expira = LocalDateTime.now().plusDays(30);
        RefreshTokenEntity guardado = persistToken("token-completo", 99L, expira, false);
        entityManager.clear();

        RefreshTokenEntity recuperado = refreshTokenRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.getToken()).isEqualTo("token-completo");
        assertThat(recuperado.getUsuarioId()).isEqualTo(99L);
        assertThat(recuperado.getExpiraEn()).isEqualTo(expira);
        assertThat(recuperado.isRevocado()).isFalse();
    }

    @Test
    void guardarToken_generaIdAutomaticamente() {
        RefreshTokenEntity guardado = persistToken("token-con-id", 1L, LocalDateTime.now().plusDays(1), false);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getId()).isPositive();
    }

    @Test
    void revocadoPorDefecto_esFalse() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("token-default");
        rt.setUsuarioId(1L);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1));
        RefreshTokenEntity guardado = entityManager.persistAndFlush(rt);
        entityManager.clear();

        RefreshTokenEntity recuperado = refreshTokenRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.isRevocado()).isFalse();
    }

    @Test
    void token_debeSerUnico() {
        persistToken("token-duplicado", 1L, LocalDateTime.now().plusDays(1), false);
        entityManager.clear();

        RefreshTokenEntity otro = new RefreshTokenEntity();
        otro.setToken("token-duplicado");
        otro.setUsuarioId(2L);
        otro.setExpiraEn(LocalDateTime.now().plusDays(2));
        otro.setRevocado(false);

        assertThatThrownBy(() -> entityManager.persistAndFlush(otro))
                .isInstanceOf(Exception.class);
    }

    @Test
    void token_nulo_lanzaExcepcion() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken(null);
        rt.setUsuarioId(1L);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1));
        rt.setRevocado(false);

        assertThatThrownBy(() -> entityManager.persistAndFlush(rt))
                .isInstanceOf(Exception.class);
    }

    @Test
    void usuarioId_nulo_lanzaExcepcion() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("token-sin-usuario");
        rt.setUsuarioId(null);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1));
        rt.setRevocado(false);

        assertThatThrownBy(() -> entityManager.persistAndFlush(rt))
                .isInstanceOf(Exception.class);
    }

    @Test
    void expiraEn_nulo_lanzaExcepcion() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("token-sin-expiracion");
        rt.setUsuarioId(1L);
        rt.setExpiraEn(null);
        rt.setRevocado(false);

        assertThatThrownBy(() -> entityManager.persistAndFlush(rt))
                .isInstanceOf(Exception.class);
    }
}
