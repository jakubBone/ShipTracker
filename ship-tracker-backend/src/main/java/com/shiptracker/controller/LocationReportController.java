package com.shiptracker.controller;

import com.shiptracker.dto.LocationReportRequest;
import com.shiptracker.dto.LocationReportResponse;
import com.shiptracker.service.LocationReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Location Reports")
@RestController
@RequestMapping("/api/ships/{shipId}/reports")
public class LocationReportController {

    private final LocationReportService locationReportService;

    public LocationReportController(LocationReportService locationReportService) {
        this.locationReportService = locationReportService;
    }

    @Operation(summary = "Get location reports for a ship")
    @ApiResponse(responseCode = "200", description = "List of reports")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Ship not found")
    @GetMapping
    public List<LocationReportResponse> getByShip(@PathVariable Long shipId) {
        return locationReportService.findByShipId(shipId);
    }

    @Operation(summary = "Add a location report for a ship")
    @ApiResponse(responseCode = "201", description = "Report created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Ship not found")
    @PostMapping
    public ResponseEntity<LocationReportResponse> create(
            @PathVariable Long shipId,
            @Valid @RequestBody LocationReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationReportService.create(shipId, request));
    }
}
