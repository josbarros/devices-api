package com.devices.api.controller;

import com.devices.api.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeviceDto> create(@Valid @RequestBody DeviceDto request) {
        var created = service.create(request.name(), request.brand(), request.state());
        return ResponseEntity.created(URI.create("/devices/" + created.getId()))
                .body(DeviceDto.from(created));
    }
}
