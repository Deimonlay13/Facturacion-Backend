package com.gdl.facturacion_backend.context;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_EMPRESA = new ThreadLocal<>();

    public static void setEmpresaId(Long empresaId) {
        CURRENT_EMPRESA.set(empresaId);
    }

    public static Long getEmpresaId() {
        return CURRENT_EMPRESA.get();
    }

    public static void clear() {
        CURRENT_EMPRESA.remove();
    }
}