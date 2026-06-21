package com.gdl.facturacion_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.dto.ProductoResponse;
import com.gdl.facturacion_backend.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "Catálogo de productos/servicios de la empresa")
public class ProductoController {

    @Autowired
    private ProductoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear producto")
    public ProductoResponse create(@RequestBody ProductoRequest request) {
        return ProductoResponse.from(service.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar productos de la empresa")
    public List<ProductoResponse> findAll() {
        return service.findAll().stream().map(ProductoResponse::from).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar producto")
    public ProductoResponse update(@PathVariable Long id,
                                   @RequestBody ProductoRequest request) {
        return ProductoResponse.from(service.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por id")
    public ProductoResponse getById(@PathVariable Long id) {
        return ProductoResponse.from(service.findById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar producto (bloquea si está en uso)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
