package com.devices.api.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTest {

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void retainsSuppliedValues(DeviceState state) {
        var device = new Device(" Phone ", " Brand ", state);

        assertThat(device.getId()).isNull();
        assertThat(device.getName()).isEqualTo(" Phone ");
        assertThat(device.getBrand()).isEqualTo(" Brand ");
        assertThat(device.getState()).isEqualTo(state);
        assertThat(device.getCreationTime()).isNull();
    }
}
