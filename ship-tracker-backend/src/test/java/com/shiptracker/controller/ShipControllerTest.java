package com.shiptracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptracker.dto.ShipRequest;
import com.shiptracker.dto.ShipResponse;
import com.shiptracker.exception.ExternalApiException;
import com.shiptracker.exception.ResourceNotFoundException;
import com.shiptracker.repository.UserRepository;
import com.shiptracker.service.NameGeneratorService;
import com.shiptracker.service.ShipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipController.class)
class ShipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShipService shipService;

    @MockitoBean
    private NameGeneratorService nameGeneratorService;

    @MockitoBean
    private UserRepository userRepository;

    private ShipResponse buildResponse(Long id, String name) {
        return new ShipResponse(id, name, LocalDate.of(2000, 1, 1), "Cargo", new BigDecimal("1000.00"), 0);
    }

    private ShipRequest buildRequest(String name) {
        return new ShipRequest(name, LocalDate.of(2000, 1, 1), "Cargo", new BigDecimal("1000.00"));
    }

    // --- GET /api/ships ---

    @Test
    @WithMockUser
    void getAll_authenticated() throws Exception {
        when(shipService.findAll()).thenReturn(List.of(buildResponse(1L, "Atlantic")));

        mockMvc.perform(get("/api/ships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Atlantic"));
    }

    @Test
    void getAll_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/ships"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/ships/{id} ---

    @Test
    @WithMockUser
    void getById_found() throws Exception {
        when(shipService.findById(1L)).thenReturn(buildResponse(1L, "Atlantic"));

        mockMvc.perform(get("/api/ships/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Atlantic"));
    }

    @Test
    @WithMockUser
    void getById_notFound() throws Exception {
        when(shipService.findById(99L)).thenThrow(new ResourceNotFoundException("Ship not found: 99"));

        mockMvc.perform(get("/api/ships/99"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/ships ---

    @Test
    @WithMockUser
    void create_valid() throws Exception {
        ShipRequest request = buildRequest("Atlantic");
        when(shipService.create(any(ShipRequest.class))).thenReturn(buildResponse(1L, "Atlantic"));

        mockMvc.perform(post("/api/ships")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Atlantic"));
    }

    @Test
    @WithMockUser
    void create_invalid() throws Exception {
        mockMvc.perform(post("/api/ships")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.tonnage").exists());
    }

    @Test
    @WithMockUser
    void create_blankName() throws Exception {
        String body = """
                {
                  "name": "   ",
                  "launchDate": "2000-01-01",
                  "shipType": "Cargo",
                  "tonnage": 1000
                }
                """;

        mockMvc.perform(post("/api/ships")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @WithMockUser
    void create_zeroTonnage() throws Exception {
        String body = """
                {
                  "name": "Atlantic",
                  "launchDate": "2000-01-01",
                  "shipType": "Cargo",
                  "tonnage": 0
                }
                """;

        mockMvc.perform(post("/api/ships")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tonnage").exists());
    }

    @Test
    @WithMockUser
    void create_negativeTonnage() throws Exception {
        String body = """
                {
                  "name": "Atlantic",
                  "launchDate": "2000-01-01",
                  "shipType": "Cargo",
                  "tonnage": -1
                }
                """;

        mockMvc.perform(post("/api/ships")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tonnage").exists());
    }

    // --- PUT /api/ships/{id} ---

    @Test
    @WithMockUser
    void update_valid() throws Exception {
        ShipRequest request = buildRequest("Updated");
        when(shipService.update(eq(1L), any(ShipRequest.class))).thenReturn(buildResponse(1L, "Updated"));

        mockMvc.perform(put("/api/ships/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @WithMockUser
    void update_notFound() throws Exception {
        when(shipService.update(eq(99L), any(ShipRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ship not found: 99"));

        mockMvc.perform(put("/api/ships/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/ships/generate-name ---

    @Test
    @WithMockUser
    void generateName_success() throws Exception {
        when(nameGeneratorService.generateName()).thenReturn("Poseidon");

        mockMvc.perform(get("/api/ships/generate-name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Poseidon"));
    }

    @Test
    void generateName_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/ships/generate-name"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void generateName_apiError() throws Exception {
        when(nameGeneratorService.generateName()).thenThrow(new ExternalApiException("API unavailable"));

        mockMvc.perform(get("/api/ships/generate-name"))
                .andExpect(status().isServiceUnavailable());
    }
}
