package com.shiptracker.repository;

import com.shiptracker.entity.LocationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationReportRepository extends JpaRepository<LocationReport, Long> {

    List<LocationReport> findByShipIdOrderByReportDateAsc(Long shipId);
}
