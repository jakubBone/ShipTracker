package com.shiptracker.service;

import com.shiptracker.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class NameGeneratorService {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;

    public NameGeneratorService(
            @Value("${randommer.api.key}") String apiKey,
            @Value("${randommer.api.url}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restClient = RestClient.create();
    }

    public String generateName() {
        try {
            String[] names = restClient.get()
                    .uri(apiUrl + "?nameType=surname&quantity=1")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(String[].class);

            if (names == null || names.length == 0) {
                throw new ExternalApiException("Name generator returned empty response");
            }
            return names[0];
        } catch (RestClientException ex) {
            throw new ExternalApiException("Name generator service is unavailable", ex);
        }
    }
}
