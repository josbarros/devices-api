package com.devices.api.service;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import com.devices.api.persistence.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository repository;

    @InjectMocks
    private DeviceService service;

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = {"AVAILABLE", "INACTIVE"})
    void deletesEligibleDevice(DeviceState state) {
        var saved = new Device("Phone", "Brand", state);
        when(repository.findById(42L)).thenReturn(Optional.of(saved));

        service.delete(42L);

        verify(repository).findById(42L);
        verify(repository).delete(saved);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsDeletionOfInUseDevice() {
        var saved = new Device("Phone", "Brand", DeviceState.IN_USE);
        when(repository.findById(42L)).thenReturn(Optional.of(saved));

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(DeviceInUseException.class)
                .hasMessage("Device 42 is in use and cannot be deleted");

        verify(repository).findById(42L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsDeletionOfMissingDevice() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessage("Device 42 not found");

        verify(repository).findById(42L);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void listsDevicesByStateAndReturnsRepositoryResult(DeviceState state) {
        var saved = List.of(new Device("Phone", "Brand", state));
        when(repository.findByState(state)).thenReturn(saved);

        assertThat(service.getAll(null, state)).isSameAs(saved);
        verify(repository).findByState(state);
    }

    @Test
    void listsNoDevicesWhenStateHasNoMatches() {
        when(repository.findByState(DeviceState.INACTIVE)).thenReturn(List.of());

        assertThat(service.getAll(null, DeviceState.INACTIVE)).isEmpty();
        verify(repository).findByState(DeviceState.INACTIVE);
    }

    @ParameterizedTest
    @CsvSource(value = {"Brand|AVAILABLE", "brand|IN_USE", " Brand |INACTIVE", "''|AVAILABLE"},
            delimiter = '|', ignoreLeadingAndTrailingWhitespace = false, emptyValue = "")
    void listsDevicesByBrandAndStateAndReturnsRepositoryResult(String brand, DeviceState state) {
        var saved = List.of(new Device("Phone", brand, state));
        when(repository.findByBrandAndState(brand, state)).thenReturn(saved);

        assertThat(service.getAll(brand, state)).isSameAs(saved);
        verify(repository).findByBrandAndState(brand, state);
    }

    @Test
    void listsNoDevicesWhenBrandAndStateHaveNoMatches() {
        when(repository.findByBrandAndState("Missing", DeviceState.AVAILABLE)).thenReturn(List.of());

        assertThat(service.getAll("Missing", DeviceState.AVAILABLE)).isEmpty();
        verify(repository).findByBrandAndState("Missing", DeviceState.AVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Brand", "brand", " Brand ", ""})
    void listsDevicesByBrandAndReturnsRepositoryResult(String brand) {
        var saved = List.of(new Device("Phone", brand, DeviceState.AVAILABLE));
        when(repository.findByBrand(brand)).thenReturn(saved);

        assertThat(service.getAll(brand, null)).isSameAs(saved);
        verify(repository).findByBrand(brand);
    }

    @Test
    void listsNoDevicesWhenBrandHasNoMatches() {
        when(repository.findByBrand("Missing")).thenReturn(List.of());

        assertThat(service.getAll("Missing", null)).isEmpty();
        verify(repository).findByBrand("Missing");
    }

    @Test
    void listsDevicesAndReturnsRepositoryResult() {
        var saved = List.of(
                new Device("Phone", "Brand", DeviceState.AVAILABLE),
                new Device("Tablet", "Other", DeviceState.IN_USE),
                new Device("Watch", "Brand", DeviceState.INACTIVE));
        when(repository.findAll()).thenReturn(saved);

        assertThat(service.getAll(null, null)).isSameAs(saved);
        verify(repository).findAll();
    }

    @Test
    void listsNoDevicesWhenRepositoryIsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.getAll(null, null)).isEmpty();
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
