package com.devices.api.domain;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, check = @CheckConstraint(
            name = "device_name_not_blank",
            constraint = "length(trim(name, char(9, 10, 11, 12, 13, 32))) > 0"))
    private String name;

    @Column(nullable = false, check = @CheckConstraint(
            name = "device_brand_not_blank",
            constraint = "length(trim(brand, char(9, 10, 11, 12, 13, 32))) > 0"))
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceState state;

    @Generated(event = EventType.INSERT)
    @ColumnDefault("(strftime('%Y-%m-%d %H:%M:%f', 'now'))")
    @Column(name = "creation_time", nullable = false, insertable = false, updatable = false)
    private Instant creationTime;

    protected Device() {
    }

    public Device(String name, String brand, DeviceState state) {
        this.name = name;
        this.brand = brand;
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public DeviceState getState() {
        return state;
    }

    public Instant getCreationTime() {
        return creationTime;
    }
}
