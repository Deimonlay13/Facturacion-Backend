package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.entity.AuditoriaEntity;
import com.gdl.facturacion_backend.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    /** Últimos 200 registros de auditoría (más recientes primero). */
    @GetMapping
    public List<AuditoriaEntity> listar() {
        return auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream().limit(200).toList();
    }
}
