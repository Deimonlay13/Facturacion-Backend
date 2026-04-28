package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.tipoDocumento.TipoDocumentoResponseDto;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoDocumentoService {

    private final TipoDocumentoRepository repository;

    public List<TipoDocumentoResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TipoDocumentoEntity findByCodigoSii(Integer codigoSii) {
        return repository.findByCodigoSii(codigoSii)
                .orElseThrow(() -> new RuntimeException("Tipo de documento no existe"));
    }

    private TipoDocumentoResponseDto toResponse(TipoDocumentoEntity entity) {
        return new TipoDocumentoResponseDto(
                entity.getId(),
                entity.getCodigoSii(),
                entity.getDescripcion()
        );
    }
}