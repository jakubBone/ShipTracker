package com.shiptracker.controller;

import com.shiptracker.dto.LocationReportRequest;
import com.shiptracker.dto.LocationReportResponse;
import com.shiptracker.service.LocationReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ships/{shipId}/reports")
public class LocationReportController {

    private final LocationReportService locationReportService;

    public LocationReportController(LocationReportService locationReportService) {
        this.locationReportService = locationReportService;
    }

    @GetMapping
    public List<LocationReportResponse> getByShip(@PathVariable Long shipId) {
        return locationReportService.findByShipId(shipId);
    }

    @PostMapping
    public ResponseEntity<LocationReportResponse> create(
            @PathVariable Long shipId,
            @Valid @RequestBody LocationReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationReportService.create(shipId, request));
    }
}
