package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Todo el cálculo de montos ocurre en el backend.
 * El frontend nunca envía subtotal, neto, IVA ni total.
 */
@Service
public class CalculoMontosService {

    private final ReglaTributariaResolver reglaResolver;

    public CalculoMontosService(ReglaTributariaResolver reglaResolver) {
        this.reglaResolver = reglaResolver;
    }

    /** Recalcula subtotales por línea y los montos del documento según la regla del tipo. */
    public void recalcular(DocumentoTributarioEntity documento) {
        ReglaTributaria regla = reglaResolver.resolver(documento.getTipoDocumento().getCodigoSii());

        BigDecimal neto = BigDecimal.ZERO;
        if (documento.getDetalles() != null) {
            for (DetalleDocumentoEntity detalle : documento.getDetalles()) {
                BigDecimal subtotal = calcularSubtotal(detalle);
                detalle.setSubtotal(subtotal);
                neto = neto.add(subtotal);
            }
        }

        neto = neto.setScale(0, RoundingMode.HALF_UP);
        BigDecimal iva = neto.multiply(regla.tasaIva()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = neto.add(iva);

        documento.setMontoNeto(neto);
        documento.setMontoIva(iva);
        documento.setMontoTotal(total);
    }

    public BigDecimal calcularSubtotal(DetalleDocumentoEntity detalle) {
        BigDecimal cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : BigDecimal.ZERO;
        BigDecimal precio = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : BigDecimal.ZERO;
        return cantidad.multiply(precio).setScale(0, RoundingMode.HALF_UP);
    }
}
