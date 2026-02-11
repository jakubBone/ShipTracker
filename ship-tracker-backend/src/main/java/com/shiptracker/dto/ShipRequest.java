package com.shiptracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShipRequest(
        @NotBlank String name,
        @NotNull LocalDate launchDate,
        @NotBlank String shipType,
        @NotNull @Positive BigDecimal tonnage
) {}
