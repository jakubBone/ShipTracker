package com.shiptracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Username", example = "admin")
        @NotBlank String username,

        @Schema(description = "Password", example = "admin123")
        @NotBlank String password
) {}
