package com.devices.api.domain;

import java.time.Instant;
import java.util.UUID;

public class Device {

    private final UUID id;
    private final String name;
    private final String brand;
    private final DeviceState state;
    private final Instant creationTime;

    public Device(UUID id, String name, String brand, DeviceState state, Instant creationTime) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.state = state;
        this.creationTime = creationTime;
    }

    public UUID getId() {
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
