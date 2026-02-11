package com.shiptracker.controller;

import com.shiptracker.dto.ShipRequest;
import com.shiptracker.dto.ShipResponse;
import com.shiptracker.service.NameGeneratorService;
import com.shiptracker.service.ShipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ships")
public class ShipController {

    private final ShipService shipService;
    private final NameGeneratorService nameGeneratorService;

    public ShipController(ShipService shipService, NameGeneratorService nameGeneratorService) {
        this.shipService = shipService;
        this.nameGeneratorService = nameGeneratorService;
    }

    @GetMapping
    public List<ShipResponse> getAll() {
        return shipService.findAll();
    }

    @GetMapping("/{id}")
    public ShipResponse getById(@PathVariable Long id) {
        return shipService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ShipResponse> create(@Valid @RequestBody ShipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipService.create(request));
    }

    @PutMapping("/{id}")
    public ShipResponse update(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
        return shipService.update(id, request);
    }

    @GetMapping("/generate-name")
    public Map<String, String> generateName() {
        return Map.of("name", nameGeneratorService.generateName());
    }
}
