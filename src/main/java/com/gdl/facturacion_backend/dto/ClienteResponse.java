package com.gdl.facturacion_backend.dto;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class ClienteResponse {
    Long id;
    String rut;
    String razonSocial;
    String nombreFantasia;
    String giro;
    String direccion;
    String ciudad;
    String comuna;
    String region;
    String pais;
    String telefono;
    String email;
    Boolean activo;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    public static ClienteResponse from(ClienteEntity e) {
        return new ClienteResponse(
                e.getId(),
                e.getRut(),
                e.getRazonSocial(),
                e.getNombreFantasia(),
                e.getGiro(),
                e.getDireccion(),
                e.getCiudad(),
                e.getComuna(),
                e.getRegion(),
                e.getPais(),
                e.getTelefono(),
                e.getEmail(),
                e.getActivo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
