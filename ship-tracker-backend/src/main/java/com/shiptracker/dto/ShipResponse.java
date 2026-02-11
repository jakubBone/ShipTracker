package com.shiptracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShipResponse(
        Long id,
        String name,
        LocalDate launchDate,
        String shipType,
        BigDecimal tonnage,
        int reportCount
) {}
