package com.devices.api.controller;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.time.Instant;

public record DeviceDto(
        @Null Long id,
        @NotBlank String name,
        @NotBlank String brand,
        @NotNull DeviceState state,
        @Null Instant creationTime) {

    public static DeviceDto from(Device device) {
        return new DeviceDto(device.getId(), device.getName(), device.getBrand(),
                device.getState(), device.getCreationTime());
    }
}
