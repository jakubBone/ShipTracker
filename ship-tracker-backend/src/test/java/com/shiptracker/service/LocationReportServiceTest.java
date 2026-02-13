package com.shiptracker.service;

import com.shiptracker.dto.LocationReportRequest;
import com.shiptracker.dto.LocationReportResponse;
import com.shiptracker.entity.LocationReport;
import com.shiptracker.entity.Ship;
import com.shiptracker.exception.ResourceNotFoundException;
import com.shiptracker.repository.LocationReportRepository;
import com.shiptracker.repository.ShipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationReportServiceTest {

    @Mock
    private LocationReportRepository locationReportRepository;

    @Mock
    private ShipRepository shipRepository;

    @InjectMocks
    private LocationReportService locationReportService;

    private Ship buildShip(Long id) {
        Ship ship = new Ship();
        ship.setId(id);
        ship.setName("Atlantic");
        ship.setLaunchDate(LocalDate.of(2000, 1, 1));
        ship.setShipType("Cargo");
        ship.setTonnage(new BigDecimal("1000.00"));
        return ship;
    }

    private LocationReport buildReport(Long id, LocalDate date) {
        LocationReport report = new LocationReport();
        report.setId(id);
        report.setReportDate(date);
        report.setCountry("Poland");
        report.setPort("Gdansk");
        return report;
    }

    // --- findByShipId ---

    @Test
    void findByShipId_shipNotFound() {
        when(shipRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> locationReportService.findByShipId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findByShipId_found() {
        LocalDate earlier = LocalDate.of(2024, 1, 1);
        LocalDate later = LocalDate.of(2024, 6, 1);
        LocationReport r1 = buildReport(1L, earlier);
        LocationReport r2 = buildReport(2L, later);

        when(shipRepository.existsById(1L)).thenReturn(true);
        when(locationReportRepository.findByShipIdOrderByReportDateAsc(1L))
                .thenReturn(List.of(r1, r2));

        List<LocationReportResponse> result = locationReportService.findByShipId(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).reportDate()).isEqualTo(earlier);
        assertThat(result.get(1).reportDate()).isEqualTo(later);
    }

    // --- create ---

    @Test
    void create_shipNotFound() {
        when(shipRepository.findById(99L)).thenReturn(Optional.empty());

        LocationReportRequest request = new LocationReportRequest(
                LocalDate.of(2024, 1, 1), "Poland", "Gdansk");

        assertThatThrownBy(() -> locationReportService.create(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_success() {
        Ship ship = buildShip(1L);
        LocationReportRequest request = new LocationReportRequest(
                LocalDate.of(2024, 3, 15), "Germany", "Hamburg");
        LocationReport saved = buildReport(10L, request.reportDate());
        saved.setCountry(request.country());
        saved.setPort(request.port());

        when(shipRepository.findById(1L)).thenReturn(Optional.of(ship));
        when(locationReportRepository.save(any(LocationReport.class))).thenReturn(saved);

        LocationReportResponse response = locationReportService.create(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.country()).isEqualTo("Germany");
        assertThat(response.port()).isEqualTo("Hamburg");
        assertThat(response.reportDate()).isEqualTo(LocalDate.of(2024, 3, 15));
    }
}