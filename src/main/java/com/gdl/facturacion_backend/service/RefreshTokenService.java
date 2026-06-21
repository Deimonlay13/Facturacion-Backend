package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.entity.RefreshTokenEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.RefreshTokenRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    /** Duración del refresh token en ms. Administrable por JWT_REFRESH_EXPIRATION. Default: 7 días. */
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    /** Crea y persiste un refresh token para el usuario. */
    public String crear(UsuarioEntity usuario) {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUsuarioId(usuario.getId());
        rt.setExpiraEn(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));
        rt.setRevocado(false);
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    /** Valida el refresh token y emite un nuevo access token (manteniendo el mismo refresh). */
    public AuthResponse refrescar(String token) {
        RefreshTokenEntity rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ReglaNegocioException("Refresh token inválido"));

        if (rt.isRevocado() || rt.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("Sesión expirada, vuelve a iniciar sesión");
        }

        UsuarioEntity usuario = usuarioRepository.findById(rt.getUsuarioId())
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        String access = jwtService.generateToken(
                usuario.getUsername(),
                usuario.getEmpresa().getId(),
                usuario.getRol().getNombre());

        return new AuthResponse(access, token);
    }

    /** Revoca el refresh token (logout). */
    public void revocar(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            refreshTokenRepository.save(rt);
        });
    }
}
