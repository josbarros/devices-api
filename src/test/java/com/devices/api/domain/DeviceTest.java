package com.devices.api.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTest {

    @ParameterizedTest
    @CsvSource({"Tablet, Brand", "Phone, Other", "Tablet, Other", "' Phone ', Brand", "Phone, ' Brand '"})
    void rejectsNameOrBrandChangesWhileDeviceRemainsInUse(String name, String brand) {
        var device = new Device("Phone", "Brand", DeviceState.IN_USE);

        assertThatThrownBy(() -> device.update(name, brand, DeviceState.IN_USE))
                .isInstanceOf(DeviceUpdateNotAllowedException.class)
                .hasMessage("The name or brand of an in-use device cannot be changed");

        assertThat(device.getName()).isEqualTo("Phone");
        assertThat(device.getBrand()).isEqualTo("Brand");
        assertThat(device.getState()).isEqualTo(DeviceState.IN_USE);
    }

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = {"AVAILABLE", "INACTIVE"})
    void allowsNameAndBrandChangesWhenReleasingInUseDevice(DeviceState state) {
        var device = new Device("Phone", "Brand", DeviceState.IN_USE);

        device.update("Tablet", "Other", state);

        assertThat(device.getName()).isEqualTo("Tablet");
        assertThat(device.getBrand()).isEqualTo("Other");
        assertThat(device.getState()).isEqualTo(state);
    }

    @Test
    void allowsUnchangedInUseDevice() {
        var device = new Device("Phone", "Brand", DeviceState.IN_USE);

        device.update("Phone", "Brand", DeviceState.IN_USE);

        assertThat(device.getName()).isEqualTo("Phone");
        assertThat(device.getBrand()).isEqualTo("Brand");
        assertThat(device.getState()).isEqualTo(DeviceState.IN_USE);
    }

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
    void replacesMutableValues(DeviceState state) {
        var device = new Device("Phone", "Brand", DeviceState.AVAILABLE);

        device.update(" Tablet ", " Other ", state);

        assertThat(device.getName()).isEqualTo(" Tablet ");
        assertThat(device.getBrand()).isEqualTo(" Other ");
        assertThat(device.getState()).isEqualTo(state);
        assertThat(device.getId()).isNull();
        assertThat(device.getCreationTime()).isNull();
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
