package com.shiptracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ships")
public class Ship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "launch_date", nullable = false)
    private LocalDate launchDate;

    @Column(name = "ship_type", nullable = false, length = 50)
    private String shipType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tonnage;

    @OneToMany(mappedBy = "ship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LocationReport> locationReports = new ArrayList<>();

    public Ship() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getLaunchDate() { return launchDate; }
    public String getShipType() { return shipType; }
    public BigDecimal getTonnage() { return tonnage; }
    public List<LocationReport> getLocationReports() { return locationReports; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLaunchDate(LocalDate launchDate) { this.launchDate = launchDate; }
    public void setShipType(String shipType) { this.shipType = shipType; }
    public void setTonnage(BigDecimal tonnage) { this.tonnage = tonnage; }
    public void setLocationReports(List<LocationReport> locationReports) { this.locationReports = locationReports; }
}
