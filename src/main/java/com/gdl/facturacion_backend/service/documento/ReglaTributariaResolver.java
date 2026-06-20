package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resuelve la {@link ReglaTributaria} aplicable según el código SII del tipo de documento.
 * Spring inyecta todas las reglas registradas; agregar un nuevo tipo = agregar un @Component.
 */
@Component
public class ReglaTributariaResolver {

    private final Map<Integer, ReglaTributaria> reglas;

    public ReglaTributariaResolver(List<ReglaTributaria> reglas) {
        this.reglas = reglas.stream()
                .collect(Collectors.toMap(ReglaTributaria::codigoSii, Function.identity()));
    }

    public ReglaTributaria resolver(Integer codigoSii) {
        ReglaTributaria regla = reglas.get(codigoSii);
        if (regla == null) {
            throw new ReglaNegocioException("Tipo de documento no soportado: " + codigoSii);
        }
        return regla;
    }
}
