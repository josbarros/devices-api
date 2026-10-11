package com.devices.api.service;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(Long id) {
        super("Device " + id + " not found");
    }
}
