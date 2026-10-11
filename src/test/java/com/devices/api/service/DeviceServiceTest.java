package com.devices.api.service;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import com.devices.api.persistence.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository repository;

    @InjectMocks
    private DeviceService service;

    @Test
    void listsDevicesAndReturnsRepositoryResult() {
        var saved = List.of(
                new Device("Phone", "Brand", DeviceState.AVAILABLE),
                new Device("Tablet", "Other", DeviceState.IN_USE),
                new Device("Watch", "Brand", DeviceState.INACTIVE));
        when(repository.findAll()).thenReturn(saved);

        assertThat(service.getAll()).isSameAs(saved);
        verify(repository).findAll();
    }

    @Test
    void listsNoDevicesWhenRepositoryIsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.getAll()).isEmpty();
        verify(repository).findAll();
    }

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void retrievesDeviceAndReturnsRepositoryResult(DeviceState state) {
        var saved = new Device("Phone", "Brand", state);
        when(repository.findById(42L)).thenReturn(Optional.of(saved));

        var retrieved = service.getById(42L);

        assertThat(retrieved).isSameAs(saved);
        verify(repository).findById(42L);
    }

    @Test
    void rejectsRetrievalOfMissingDevice() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(42L))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessage("Device 42 not found");
    }

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void createsDeviceAndReturnsRepositoryResult(DeviceState state) {
        var saved = new Device(" Phone ", " Brand ", state);
        when(repository.save(any(Device.class))).thenReturn(saved);

        var created = service.create(" Phone ", " Brand ", state);

        verify(repository).save(argThat(device ->
                " Phone ".equals(device.getName())
                        && " Brand ".equals(device.getBrand())
                        && device.getState() == state
                        && device.getId() == null
                        && device.getCreationTime() == null));
        assertThat(created).isSameAs(saved);
    }
}
