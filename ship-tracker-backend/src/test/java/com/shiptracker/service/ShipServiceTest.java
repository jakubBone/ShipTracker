package com.shiptracker.service;

import com.shiptracker.dto.ShipRequest;
import com.shiptracker.dto.ShipResponse;
import com.shiptracker.entity.Ship;
import com.shiptracker.exception.ResourceNotFoundException;
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
class ShipServiceTest {

    @Mock
    private ShipRepository shipRepository;

    @InjectMocks
    private ShipService shipService;

    private Ship buildShip(Long id, String name, int reportCount) {
        Ship ship = new Ship();
        ship.setId(id);
        ship.setName(name);
        ship.setLaunchDate(LocalDate.of(2000, 1, 1));
        ship.setShipType("Cargo");
        ship.setTonnage(new BigDecimal("1000.00"));
        // locationReports is empty ArrayList by default (size() == reportCount only when need to add them)
        // for simplicity reportCount is modelled via the default empty list (size == 0) or populated list
        for (int i = 0; i < reportCount; i++) {
            ship.getLocationReports().add(null); // placeholder to inflate size
        }
        return ship;
    }

    private ShipRequest buildRequest(String name) {
        return new ShipRequest(name, LocalDate.of(2000, 1, 1), "Cargo", new BigDecimal("1000.00"));
    }

    // --- findAll ---

    @Test
    void findAll_whenEmpty() {
        when(shipRepository.findAll()).thenReturn(List.of());

        List<ShipResponse> result = shipService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_whenShipsExist() {
        Ship ship = buildShip(1L, "Atlantic", 3);
        when(shipRepository.findAll()).thenReturn(List.of(ship));

        List<ShipResponse> result = shipService.findAll();

        assertThat(result).hasSize(1);
        ShipResponse response = result.getFirst();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Atlantic");
        assertThat(response.shipType()).isEqualTo("Cargo");
        assertThat(response.reportCount()).isEqualTo(3);
    }

    // --- findById ---

    @Test
    void findById_found() {
        Ship ship = buildShip(1L, "Atlantic", 0);
        when(shipRepository.findById(1L)).thenReturn(Optional.of(ship));

        ShipResponse response = shipService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Atlantic");
    }

    @Test
    void findById_notFound() {
        when(shipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- create ---

    @Test
    void create() {
        ShipRequest request = buildRequest("Atlantic");
        Ship saved = buildShip(1L, "Atlantic", 0);
        when(shipRepository.save(any(Ship.class))).thenReturn(saved);

        ShipResponse response = shipService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Atlantic");
    }

    // --- update ---

    @Test
    void update_found() {
        Ship existing = buildShip(1L, "OldName", 0);
        Ship saved = buildShip(1L, "NewName", 0);
        when(shipRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shipRepository.save(existing)).thenReturn(saved);

        ShipResponse response = shipService.update(1L, buildRequest("NewName"));

        assertThat(response.name()).isEqualTo("NewName");
    }

    @Test
    void update_notFound() {
        when(shipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipService.update(99L, buildRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
