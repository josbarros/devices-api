package com.devices.api.persistence;

import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByBrand(String brand);

    List<Device> findByState(DeviceState state);

    List<Device> findByBrandAndState(String brand, DeviceState state);
}
