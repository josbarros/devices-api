package com.devices.api.persistence;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository repository;

    @ParameterizedTest
    @ValueSource(strings = {"Brand", "brand", " Brand ", ""})
    void findsDevicesByExactBrand(String brand) {
        repository.deleteAll();
        var phone = repository.saveAndFlush(new Device("Phone", "Brand", DeviceState.AVAILABLE));
        var tablet = repository.saveAndFlush(new Device("Tablet", "Brand", DeviceState.IN_USE));
        var watch = repository.saveAndFlush(new Device("Watch", "brand", DeviceState.INACTIVE));
        var speaker = repository.saveAndFlush(new Device("Speaker", " Brand ", DeviceState.AVAILABLE));

        var expected = switch (brand) {
            case "Brand" -> List.of(phone.getId(), tablet.getId());
            case "brand" -> List.of(watch.getId());
            case " Brand " -> List.of(speaker.getId());
            default -> List.<Long>of();
        };
        assertThat(repository.findByBrand(brand))
                .extracting(Device::getId)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void databaseGeneratesIntegerIdAndCreationTime() {
        var entity = new Device("Phone", "Brand", DeviceState.AVAILABLE);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreationTime()).isNull();

        var before = Instant.now().minusSeconds(1);
        var saved = repository.saveAndFlush(entity);

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getCreationTime()).isBetween(before, Instant.now());
        assertThat(repository.findById(saved.getId()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(saved);
    }

    @Test
    void rejectsNullName() {
        assertRejected(null, "Brand", DeviceState.AVAILABLE);
    }

    @Test
    void rejectsBlankName() {
        assertRejected("", "Brand", DeviceState.AVAILABLE);
        assertRejected(" \t\n\r ", "Brand", DeviceState.AVAILABLE);
    }

    @Test
    void rejectsNullBrand() {
        assertRejected("Phone", null, DeviceState.AVAILABLE);
    }

    @Test
    void rejectsBlankBrand() {
        assertRejected("Phone", "", DeviceState.AVAILABLE);
        assertRejected("Phone", " \t\n\r ", DeviceState.AVAILABLE);
    }

    @Test
    void rejectsNullState() {
        assertRejected("Phone", "Brand", null);
    }

    private void assertRejected(String name, String brand, DeviceState state) {
        assertThatThrownBy(() -> repository.saveAndFlush(new Device(name, brand, state)))
                .isInstanceOf(DataAccessException.class);
    }
}
