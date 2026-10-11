package com.devices.api.persistence;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository repository;

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
