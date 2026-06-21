package com.gdl.facturacion_backend.config;

import com.gdl.facturacion_backend.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registra automáticamente en auditoría las operaciones de escritura de los servicios
 * (crear/editar/eliminar/cambiar/emitir/importar...). No guarda los valores de los
 * argumentos para no filtrar datos sensibles (ej. contraseñas): solo el id del resultado
 * o los tipos de argumento.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private static final Set<String> VERBOS = Set.of(
            "create", "crear", "save", "guardar", "update", "actualizar",
            "delete", "eliminar", "desactivar", "cambiar", "importar",
            "register", "emitir", "agregar");

    private final AuditoriaService auditoriaService;

    @AfterReturning(
            pointcut = "execution(* com.gdl.facturacion_backend.service..*(..)) "
                    + "&& !within(com.gdl.facturacion_backend.service.AuditoriaService)",
            returning = "result")
    public void auditar(JoinPoint jp, Object result) {
        String metodo = jp.getSignature().getName();
        String lower = metodo.toLowerCase();
        boolean esEscritura = VERBOS.stream().anyMatch(lower::startsWith);
        if (!esEscritura) return;

        String tabla = jp.getSignature().getDeclaringType().getSimpleName()
                .replaceAll("(ServiceImpl|Service)$", "");
        auditoriaService.registrar(tabla, metodo, detalle(jp, result));
    }

    private String detalle(JoinPoint jp, Object result) {
        if (result != null) {
            try {
                Object id = result.getClass().getMethod("getId").invoke(result);
                if (id != null) return "id=" + id;
            } catch (Exception ignored) {
                // el resultado no tiene getId()
            }
        }
        return "args=" + Arrays.stream(jp.getArgs())
                .map(a -> a == null ? "null" : a.getClass().getSimpleName())
                .collect(Collectors.joining(","));
    }
}
