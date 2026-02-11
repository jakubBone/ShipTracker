package com.shiptracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NameGeneratorService {

    private final RestClient restClient;
    private final String apiKey;

    public NameGeneratorService(@Value("${randommer.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public String generateName() {
        String[] names = restClient.get()
                .uri("https://randommer.io/api/Name?nameType=surname&quantity=1")
                .header("X-Api-Key", apiKey)
                .retrieve()
                .body(String[].class);

        if (names == null || names.length == 0) {
            throw new RuntimeException("Name generator returned empty response");
        }
        return names[0];
    }
}
