package com.devices.api.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTest {

    @ParameterizedTest
    @CsvSource({"available, AVAILABLE", "in-use, IN_USE", "inactive, INACTIVE"})
    void mapsStateToAndFromApiJson(String value, DeviceState state) {
        var mapper = JsonMapper.builder().build();
        var json = "\"" + value + "\"";

        assertThat(mapper.writeValueAsString(state)).isEqualTo(json);
        assertThat(mapper.readValue(json, DeviceState.class)).isEqualTo(state);
    }

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
