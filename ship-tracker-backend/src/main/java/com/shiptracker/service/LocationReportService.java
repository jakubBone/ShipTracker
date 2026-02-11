package com.shiptracker.service;

import com.shiptracker.dto.LocationReportRequest;
import com.shiptracker.dto.LocationReportResponse;
import com.shiptracker.entity.LocationReport;
import com.shiptracker.entity.Ship;
import com.shiptracker.exception.ResourceNotFoundException;
import com.shiptracker.repository.LocationReportRepository;
import com.shiptracker.repository.ShipRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationReportService {

    private final LocationReportRepository locationReportRepository;
    private final ShipRepository shipRepository;

    public LocationReportService(LocationReportRepository locationReportRepository,
                                 ShipRepository shipRepository) {
        this.locationReportRepository = locationReportRepository;
        this.shipRepository = shipRepository;
    }

    public List<LocationReportResponse> findByShipId(Long shipId) {
        if (!shipRepository.existsById(shipId)) {
            throw new ResourceNotFoundException("Ship not found with id: " + shipId);
        }
        return locationReportRepository.findByShipIdOrderByReportDateAsc(shipId).stream()
                .map(this::toResponse)
                .toList();
    }

    public LocationReportResponse create(Long shipId, LocationReportRequest dto) {
        Ship ship = shipRepository.findById(shipId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship not found with id: " + shipId));

        LocationReport report = new LocationReport();
        report.setShip(ship);
        report.setReportDate(dto.reportDate());
        report.setCountry(dto.country());
        report.setPort(dto.port());

        return toResponse(locationReportRepository.save(report));
    }

    private LocationReportResponse toResponse(LocationReport report) {
        return new LocationReportResponse(
                report.getId(),
                report.getReportDate(),
                report.getCountry(),
                report.getPort()
        );
    }
}
