package com.devices.api.service;

public class DeviceInUseException extends RuntimeException {

    public DeviceInUseException(Long id) {
        super("Device " + id + " is in use and cannot be deleted");
    }
}
