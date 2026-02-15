package com.shiptracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShipRequest(
        @Schema(description = "Ship name", example = "Atlantic Pioneer")
        @NotBlank String name,

        @Schema(description = "Launch date", example = "2015-06-20")
        @NotNull LocalDate launchDate,

        @Schema(description = "Ship type", example = "Cargo")
        @NotBlank String shipType,

        @Schema(description = "Tonnage in metric tons", example = "75000.00")
        @NotNull @Positive BigDecimal tonnage
) {}
