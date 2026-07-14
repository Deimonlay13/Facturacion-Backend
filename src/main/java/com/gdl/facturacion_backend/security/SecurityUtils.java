package com.gdl.facturacion_backend.security;

import com.gdl.facturacion_backend.context.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilidades de seguridad basadas en el {@link SecurityContextHolder}.
 *
 * <p>Un usuario SUPER_ADMIN es global: no pertenece a ninguna empresa, por lo que su
 * JWT no lleva empresaId y {@code TenantContext} queda vacío. Los servicios acotados por
 * empresa usan {@link #esSuperAdmin()} para, en ese caso, operar sobre todas las empresas
 * en vez de fallar por falta de tenant.
 */
public final class SecurityUtils {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private SecurityUtils() {
    }

    /** {@code true} si el usuario autenticado tiene la authority ROLE_SUPER_ADMIN. */
    public static boolean esSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_SUPER_ADMIN.equals(a.getAuthority()));
    }

    /**
     * {@code true} si es SUPER_ADMIN y NO hay empresa seleccionada en el contexto (ni X-Tenant-ID ni
     * empresa en el JWT). Es la condición de "vista global": ver datos de todas las empresas.
     * Si el super_admin elige una empresa (X-Tenant-ID), deja de ser global y queda acotado a ella.
     */
    public static boolean esSuperAdminSinEmpresa() {
        return TenantContext.getEmpresaId() == null && esSuperAdmin();
    }
}
