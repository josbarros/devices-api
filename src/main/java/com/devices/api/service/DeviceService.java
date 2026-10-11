package com.devices.api.service;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import com.devices.api.persistence.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public Device create(String name, String brand, DeviceState state) {
        return repository.save(new Device(name, brand, state));
    }

    public Device getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Transactional
    public void delete(Long id) {
        var device = getById(id);
        if (device.getState() == DeviceState.IN_USE) {
            throw new DeviceInUseException(id);
        }
        repository.delete(device);
    }

    public List<Device> getAll(String brand, DeviceState state) {
        if (brand == null && state == null) {
            return repository.findAll();
        }
        if (state == null) {
            return repository.findByBrand(brand);
        }
        if (brand == null) {
            return repository.findByState(state);
        }
        return repository.findByBrandAndState(brand, state);
    }
}
