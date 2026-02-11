package com.shiptracker.dto;

import java.time.LocalDate;

public record LocationReportResponse(
        Long id,
        LocalDate reportDate,
        String country,
        String port
) {}
