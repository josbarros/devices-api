package com.devices.api.persistence;

import com.devices.api.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByBrand(String brand);
}
