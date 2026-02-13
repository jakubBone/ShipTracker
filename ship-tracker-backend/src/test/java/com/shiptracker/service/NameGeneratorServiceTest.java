package com.shiptracker.service;

import com.shiptracker.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked") // RequestHeadersUriSpec and RequestHeadersSpec are generic — Mockito requires raw types here, warning is harmless
class NameGeneratorServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private NameGeneratorService nameGeneratorService;

    @BeforeEach
    void setUp() {
        nameGeneratorService = new NameGeneratorService(restClient, "test-key", "https://api.example.com/names");
    }

    private void stubChain() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void generateName_success() {
        stubChain();
        when(responseSpec.body(String[].class)).thenReturn(new String[]{"Atlantic"});

        String result = nameGeneratorService.generateName();

        assertThat(result).isEqualTo("Atlantic");
    }

    @Test
    void generateName_emptyResponse() {
        stubChain();
        when(responseSpec.body(String[].class)).thenReturn(new String[]{});

        assertThatThrownBy(() -> nameGeneratorService.generateName())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void generateName_apiError() {
        when(restClient.get()).thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> nameGeneratorService.generateName())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("unavailable");
    }
}
