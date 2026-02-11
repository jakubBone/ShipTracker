package com.shiptracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LocationReportRequest(
        @NotNull LocalDate reportDate,
        @NotBlank String country,
        @NotBlank String port
) {}
