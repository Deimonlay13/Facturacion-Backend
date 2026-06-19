package com.gdl.facturacion_backend.client;

import com.gdl.facturacion_backend.dto.SreCompanyResponse;
import com.gdl.facturacion_backend.exception.SreApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
public class SreClient {

    private final RestTemplate restTemplate;

    @Value("${sre.api.url}")
    private String sreUrl;

    @Value("${sre.api.token}")
    private String sreToken;

    public SreClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<SreCompanyResponse> buscarPorRut(String rut) {
        String url = UriComponentsBuilder.fromUriString(sreUrl)
                .queryParam("token", sreToken)
                .queryParam("rut", rut)
                .queryParam("actualizado", "False")
                .toUriString();

        try {
            SreCompanyResponse response = restTemplate.getForObject(url, SreCompanyResponse.class);
            if (response == null || response.hasError()) {
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (RestClientException ex) {
            throw new SreApiException("No se pudo conectar con la API de SRE: " + ex.getMessage(), ex);
        }
    }
}
