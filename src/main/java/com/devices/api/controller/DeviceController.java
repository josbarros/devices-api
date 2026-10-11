package com.devices.api.controller;

import com.devices.api.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @GetMapping
    public List<DeviceDto> getAll() {
        return service.getAll().stream().map(DeviceDto::from).toList();
    }

    @GetMapping(params = "brand")
    public List<DeviceDto> getByBrand(@RequestParam String brand) {
        return service.getAll(brand).stream().map(DeviceDto::from).toList();
    }

    @GetMapping("/{id}")
    public DeviceDto getById(@PathVariable Long id) {
        return DeviceDto.from(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<DeviceDto> create(@Valid @RequestBody DeviceDto request) {
        var created = service.create(request.name(), request.brand(), request.state());
        return ResponseEntity.created(URI.create("/devices/" + created.getId()))
                .body(DeviceDto.from(created));
    }
}
