package com.shiptracker.service;

import com.shiptracker.dto.ShipRequest;
import com.shiptracker.dto.ShipResponse;
import com.shiptracker.entity.Ship;
import com.shiptracker.exception.ResourceNotFoundException;
import com.shiptracker.repository.ShipRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ShipService {

    private final ShipRepository shipRepository;

    public ShipService(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    public List<ShipResponse> findAll() {
        return shipRepository.findAll(Sort.by("name")).stream()
                .map(this::toResponse)
                .toList();
    }

    public ShipResponse findById(Long id) {
        return shipRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ship not found with id: " + id));
    }

    public ShipResponse create(ShipRequest dto) {
        Ship ship = new Ship();
        ship.setName(dto.name());
        ship.setLaunchDate(dto.launchDate());
        ship.setShipType(dto.shipType());
        ship.setTonnage(dto.tonnage());
        return toResponse(shipRepository.save(ship));
    }

    public ShipResponse update(Long id, ShipRequest dto) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ship not found with id: " + id));
        ship.setName(dto.name());
        ship.setLaunchDate(dto.launchDate());
        ship.setShipType(dto.shipType());
        ship.setTonnage(dto.tonnage());
        return toResponse(shipRepository.save(ship));
    }

    private ShipResponse toResponse(Ship ship) {
        return new ShipResponse(
                ship.getId(),
                ship.getName(),
                ship.getLaunchDate(),
                ship.getShipType(),
                ship.getTonnage(),
                ship.getLocationReports().size()
        );
    }
}
