package com.devices.api.service;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import com.devices.api.persistence.DeviceRepository;
import org.springframework.stereotype.Service;

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

    public List<Device> getAll() {
        return repository.findAll();
    }

    public List<Device> getAll(String brand) {
        return repository.findByBrand(brand);
    }
}
