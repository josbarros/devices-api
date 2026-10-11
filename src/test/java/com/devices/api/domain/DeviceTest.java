package com.devices.api.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTest {

    private static final UUID ID = UUID.fromString("a7e285cf-4964-43fa-987c-871e45e06808");
    private static final Instant CREATION_TIME = Instant.parse("2026-10-11T12:00:00Z");

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void retainsSuppliedValues(DeviceState state) {
        var device = new Device(ID, " Phone ", " Brand ", state, CREATION_TIME);

        assertThat(device.getId()).isEqualTo(ID);
        assertThat(device.getName()).isEqualTo(" Phone ");
        assertThat(device.getBrand()).isEqualTo(" Brand ");
        assertThat(device.getState()).isEqualTo(state);
        assertThat(device.getCreationTime()).isEqualTo(CREATION_TIME);
    }
}
