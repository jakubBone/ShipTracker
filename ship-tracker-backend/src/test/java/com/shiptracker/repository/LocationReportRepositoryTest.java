package com.shiptracker.repository;

import com.shiptracker.entity.LocationReport;
import com.shiptracker.entity.Ship;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LocationReportRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LocationReportRepository repository;

    @Test
    void findByShipIdOrderByReportDateAsc_ordered() {
        Ship ship = new Ship();
        ship.setName("Atlantic");
        ship.setLaunchDate(LocalDate.of(2010, 1, 1));
        ship.setShipType("Cargo");
        ship.setTonnage(new BigDecimal("5000.00"));
        em.persist(ship);

        LocationReport newer = new LocationReport();
        newer.setShip(ship);
        newer.setReportDate(LocalDate.of(2024, 6, 1));
        newer.setCountry("Germany");
        newer.setPort("Hamburg");
        em.persist(newer);

        LocationReport older = new LocationReport();
        older.setShip(ship);
        older.setReportDate(LocalDate.of(2024, 1, 1));
        older.setCountry("Poland");
        older.setPort("Gdansk");
        em.persist(older);

        em.flush();

        List<LocationReport> result = repository.findByShipIdOrderByReportDateAsc(ship.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReportDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.get(1).getReportDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    void findByShipIdOrderByReportDateAsc_empty() {
        Ship ship = new Ship();
        ship.setName("Pacific");
        ship.setLaunchDate(LocalDate.of(2015, 3, 15));
        ship.setShipType("Tanker");
        ship.setTonnage(new BigDecimal("8000.00"));
        em.persist(ship);
        em.flush();

        List<LocationReport> result = repository.findByShipIdOrderByReportDateAsc(ship.getId());

        assertThat(result).isEmpty();
    }
}
