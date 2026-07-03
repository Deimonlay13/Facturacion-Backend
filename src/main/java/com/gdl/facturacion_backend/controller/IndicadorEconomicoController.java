package com.gdl.facturacion_backend.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indicadores-economicos")
public class IndicadorEconomicoController {

    private static final URI MINDICADOR_API = URI.create("https://mindicador.cl/api");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> findAll() {
        try {
            HttpRequest request = HttpRequest.newBuilder(MINDICADOR_API)
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .header("User-Agent", "facturacion-backend")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("mindicador.cl respondio con estado " + response.statusCode());
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .cacheControl(CacheControl.noCache())
                    .body(response.body());
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible consultar los indicadores economicos", ex);
        }
    }
}
