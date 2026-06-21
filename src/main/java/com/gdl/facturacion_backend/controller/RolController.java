package com.gdl.facturacion_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gdl.facturacion_backend.dto.RolRequest;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.service.RolService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/roles")
@RequiredArgsConstructor 
public class RolController {

    private final RolService rolService;

    @PostMapping
    public ResponseEntity<RolEntity> crear(@RequestBody RolRequest request) {
        return ResponseEntity.ok(rolService.save(request));
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(rolService.findAll());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
    }
}