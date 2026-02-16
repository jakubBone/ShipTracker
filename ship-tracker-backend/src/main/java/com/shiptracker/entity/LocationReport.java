package com.shiptracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "location_reports")
public class LocationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ship_id", nullable = false)
    private Ship ship;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String port;

    public LocationReport() {}

    public Long getId() { return id; }
    public Ship getShip() { return ship; }
    public LocalDate getReportDate() { return reportDate; }
    public String getCountry() { return country; }
    public String getPort() { return port; }

    public void setShip(Ship ship) { this.ship = ship; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public void setCountry(String country) { this.country = country; }
    public void setPort(String port) { this.port = port; }
}
