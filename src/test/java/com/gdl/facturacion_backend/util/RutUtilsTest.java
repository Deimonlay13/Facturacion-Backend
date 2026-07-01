package com.gdl.facturacion_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test JUnit 5 puro (sin Spring, sin BD) para {@link RutUtils}.
 * Cubre RUTs validos, invalidos, con puntos, DV K/0, nulos y vacios,
 * ademas del metodo {@link RutUtils#clean(String)}.
 */
class RutUtilsTest {

    // ------------------------------------------------------------------ isValid: validos

    @Test
    @DisplayName("RUT valido con DV numerico")
    void isValid_rutNumericoValido() {
        // 12.345.678-5 es un RUT con DV correcto
        assertThat(RutUtils.isValid("12345678-5")).isTrue();
    }

    @Test
    @DisplayName("RUT valido con puntos se limpia antes de validar")
    void isValid_conPuntos() {
        assertThat(RutUtils.isValid("12.345.678-5")).isTrue();
    }

    @Test
    @DisplayName("RUT valido con DV K (minuscula) se normaliza a mayuscula")
    void isValid_dvKMinuscula() {
        // 4.674.795-K -> validado correctamente con k minuscula
        assertThat(RutUtils.isValid("4674795-k")).isTrue();
    }

    @Test
    @DisplayName("RUT valido con DV K mayuscula")
    void isValid_dvKMayuscula() {
        assertThat(RutUtils.isValid("4674795-K")).isTrue();
    }

    @Test
    @DisplayName("RUT valido cuyo DV calculado es 0")
    void isValid_dvCero() {
        // 14-0: el algoritmo produce resto 11 -> DV "0"
        assertThat(RutUtils.isValid("14-0")).isTrue();
    }

    @Test
    @DisplayName("RUT valido con espacios alrededor (se hace trim)")
    void isValid_conEspacios() {
        assertThat(RutUtils.isValid("  12.345.678-5  ")).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Varios RUTs validos conocidos")
    @ValueSource(strings = {"11111111-1", "22222222-2", "1-9", "12345678-5", "4674795-K"})
    void isValid_variosValidos(String rut) {
        assertThat(RutUtils.isValid(rut)).isTrue();
    }

    // ------------------------------------------------------------------ isValid: invalidos

    @Test
    @DisplayName("RUT con DV incorrecto es invalido")
    void isValid_dvIncorrecto() {
        assertThat(RutUtils.isValid("12345678-9")).isFalse();
    }

    @Test
    @DisplayName("RUT sin guion es invalido")
    void isValid_sinGuion() {
        assertThat(RutUtils.isValid("123456785")).isFalse();
    }

    @Test
    @DisplayName("RUT con DV no permitido (letra distinta de K) es invalido")
    void isValid_dvLetraInvalida() {
        assertThat(RutUtils.isValid("12345678-Z")).isFalse();
    }

    @Test
    @DisplayName("RUT con parte numerica demasiado larga (>8 digitos) es invalido")
    void isValid_numeroMuyLargo() {
        assertThat(RutUtils.isValid("123456789-5")).isFalse();
    }

    @Test
    @DisplayName("RUT con texto basura es invalido")
    void isValid_textoBasura() {
        assertThat(RutUtils.isValid("no-es-rut")).isFalse();
    }

    @ParameterizedTest
    @DisplayName("RUT null o en blanco es invalido")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void isValid_nullVacioOBlanco(String rut) {
        assertThat(RutUtils.isValid(rut)).isFalse();
    }

    // ------------------------------------------------------------------ clean

    @Test
    @DisplayName("clean elimina puntos, hace trim y pasa el DV a mayuscula")
    void clean_normaliza() {
        assertThat(RutUtils.clean("  4.674.795-k  ")).isEqualTo("4674795-K");
    }

    @Test
    @DisplayName("clean sobre un RUT ya limpio lo deja igual")
    void clean_idempotente() {
        String limpio = RutUtils.clean("12345678-5");
        assertThat(RutUtils.clean(limpio)).isEqualTo("12345678-5");
    }
}
