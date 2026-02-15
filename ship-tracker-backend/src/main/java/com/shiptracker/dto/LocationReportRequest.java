package com.shiptracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LocationReportRequest(
        @Schema(description = "Date of the location report", example = "2024-03-15")
        @NotNull LocalDate reportDate,

        @Schema(description = "Country where the ship was located", example = "Poland")
        @NotBlank String country,

        @Schema(description = "Port name", example = "Gdańsk")
        @NotBlank String port
) {}
