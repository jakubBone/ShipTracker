package com.shiptracker.controller;

import com.shiptracker.dto.GeneratedNameResponse;
import com.shiptracker.dto.ShipRequest;
import com.shiptracker.dto.ShipResponse;
import com.shiptracker.service.NameGeneratorService;
import com.shiptracker.service.ShipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Ships")
@RestController
@RequestMapping("/api/ships")
public class ShipController {

    private final ShipService shipService;
    private final NameGeneratorService nameGeneratorService;

    public ShipController(ShipService shipService, NameGeneratorService nameGeneratorService) {
        this.shipService = shipService;
        this.nameGeneratorService = nameGeneratorService;
    }

    @Operation(summary = "Get all ships")
    @ApiResponse(responseCode = "200", description = "List of ships")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public List<ShipResponse> getAll() {
        return shipService.findAll();
    }

    @Operation(summary = "Get ship by ID")
    @ApiResponse(responseCode = "200", description = "Ship found")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Ship not found")
    @GetMapping("/{id}")
    public ShipResponse getById(@PathVariable Long id) {
        return shipService.findById(id);
    }

    @Operation(summary = "Create a new ship")
    @ApiResponse(responseCode = "201", description = "Ship created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping
    public ResponseEntity<ShipResponse> create(@Valid @RequestBody ShipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipService.create(request));
    }

    @Operation(summary = "Update ship")
    @ApiResponse(responseCode = "200", description = "Ship updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Ship not found")
    @PutMapping("/{id}")
    public ShipResponse update(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
        return shipService.update(id, request);
    }

    @Operation(summary = "Generate a random ship name")
    @ApiResponse(responseCode = "200", description = "Generated name")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "503", description = "External API unavailable")
    @GetMapping("/generate-name")
    public GeneratedNameResponse generateName() {
        return new GeneratedNameResponse(nameGeneratorService.generateName());
    }
}
