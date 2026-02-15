package com.shiptracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptracker.dto.LocationReportRequest;
import com.shiptracker.dto.LocationReportResponse;
import com.shiptracker.exception.ResourceNotFoundException;
import com.shiptracker.repository.UserRepository;
import com.shiptracker.service.LocationReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationReportController.class)
class LocationReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationReportService locationReportService;

    @MockitoBean
    private UserRepository userRepository;

    private LocationReportResponse buildResponse() {
        return new LocationReportResponse(1L, LocalDate.of(2024, 6, 1), "Poland", "Gdansk");
    }

    // --- GET /api/ships/{shipId}/reports ---

    @Test
    @WithMockUser
    void getByShip_found() throws Exception {
        when(locationReportService.findByShipId(1L)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/ships/1/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].country").value("Poland"))
                .andExpect(jsonPath("$[0].port").value("Gdansk"));
    }

    @Test
    @WithMockUser
    void getByShip_shipNotFound() throws Exception {
        when(locationReportService.findByShipId(99L))
                .thenThrow(new ResourceNotFoundException("Ship not found: 99"));

        mockMvc.perform(get("/api/ships/99/reports"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/ships/{shipId}/reports ---

    @Test
    @WithMockUser
    void create_valid() throws Exception {
        LocationReportRequest request = new LocationReportRequest(
                LocalDate.of(2024, 6, 1), "Poland", "Gdansk");
        when(locationReportService.create(eq(1L), any(LocationReportRequest.class)))
                .thenReturn(buildResponse());

        mockMvc.perform(post("/api/ships/1/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.country").value("Poland"))
                .andExpect(jsonPath("$.port").value("Gdansk"));
    }

    @Test
    @WithMockUser
    void create_invalid() throws Exception {
        mockMvc.perform(post("/api/ships/1/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reportDate").exists())
                .andExpect(jsonPath("$.country").exists())
                .andExpect(jsonPath("$.port").exists());
    }

    @Test
    @WithMockUser
    void create_shipNotFound() throws Exception {
        LocationReportRequest request = new LocationReportRequest(
                LocalDate.of(2024, 6, 1), "Poland", "Gdansk");
        when(locationReportService.create(eq(99L), any(LocationReportRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ship not found: 99"));

        mockMvc.perform(post("/api/ships/99/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
